"""HTTP access to SofaScore's JSON API.

Python's own HTTP stack is refused. `requests` and `urllib` both receive 403 even when
sent a complete set of browser headers; `curl` receives 200 with no User-Agent at all.
What is being filtered is the TLS fingerprint of the client, not what it claims to be.
This module therefore shells out to curl, which identifies itself honestly and is
accepted. Nothing here pretends to be a browser.

That is a fragile foundation by construction, which is exactly why every HTTP concern
lives in this one file: the rest of the adapter works on parsed JSON and has no idea how
it arrived. Replacing the source means replacing this module and `transform`, not the
ingestion, the schema or the API.
"""

from __future__ import annotations

import json
import subprocess
import time
from typing import Any

BASE_URL = "https://api.sofascore.com/api/v1"

# SofaScore calls a competition a "unique tournament". 238 is the Primeira Liga.
PRIMEIRA_LIGA = 238


class FetchError(RuntimeError):
    """The API could not be reached, or answered with something that is not JSON."""


class SofaScoreClient:
    """One request at a time, paced, with a bounded number of retries.

    The pacing is not politeness theatre: a full-season backfill is roughly 650 requests,
    and sending those as fast as the network allows is both rude and the surest way to be
    blocked.
    """

    def __init__(self, delay_seconds: float = 1.0, attempts: int = 3, timeout: int = 20) -> None:
        self._delay = delay_seconds
        self._attempts = attempts
        self._timeout = timeout
        self._last_request_at = 0.0

    def seasons(self, tournament_id: int = PRIMEIRA_LIGA) -> list[dict]:
        return self._get(f"/unique-tournament/{tournament_id}/seasons")["seasons"]

    def round_events(self, season_id: int, matchday: int,
                     tournament_id: int = PRIMEIRA_LIGA) -> list[dict]:
        path = f"/unique-tournament/{tournament_id}/season/{season_id}/events/round/{matchday}"
        return self._get(path)["events"]

    def lineups(self, event_id: int) -> dict:
        return self._get(f"/event/{event_id}/lineups")

    def incidents(self, event_id: int) -> list[dict]:
        return self._get(f"/event/{event_id}/incidents")["incidents"]

    def _get(self, path: str) -> Any:
        last_error: str = ""
        for attempt in range(1, self._attempts + 1):
            self._throttle()
            body, status, error = self._curl(path)
            if error:
                last_error = error
            elif status != 200:
                last_error = f"HTTP {status}"
            else:
                try:
                    return json.loads(body)
                except json.JSONDecodeError as exc:
                    # A 200 that is not JSON usually means an interstitial page rather
                    # than data, so retrying is worth one more attempt.
                    last_error = f"response was not JSON: {exc}"
            if attempt < self._attempts:
                time.sleep(self._delay * 2 ** attempt)
        raise FetchError(f"GET {path} failed after {self._attempts} attempts: {last_error}")

    def _curl(self, path: str) -> tuple[str, int, str]:
        try:
            result = subprocess.run(
                [
                    "curl", "-sS", "--compressed",
                    "--max-time", str(self._timeout),
                    "-H", "Accept: application/json",
                    "-w", "\n%{http_code}",
                    BASE_URL + path,
                ],
                capture_output=True, text=True, timeout=self._timeout + 10,
            )
        except subprocess.TimeoutExpired:
            return "", 0, "curl timed out"
        except FileNotFoundError:
            # No point retrying this one, but the caller's message is clear enough.
            return "", 0, "curl is not installed"

        body, _, status = result.stdout.rpartition("\n")
        if result.returncode != 0:
            return "", 0, f"curl exited {result.returncode}: {result.stderr.strip()}"
        return body, int(status or 0), ""

    def _throttle(self) -> None:
        elapsed = time.monotonic() - self._last_request_at
        if elapsed < self._delay:
            time.sleep(self._delay - elapsed)
        self._last_request_at = time.monotonic()
