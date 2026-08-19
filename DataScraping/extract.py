#!/usr/bin/env python3
"""Extracts one or more matchdays of the Primeira Liga into the canonical format.

    python3 extract.py --season 2025/26 --matchday 1 --out round-1.json
    python3 extract.py --season 2025/26 --matchday 1 --through 34 --out season.json

Exits non-zero and writes nothing when validation fails, so a scheduled run reports a
red build instead of quietly ingesting nonsense.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import sys

from sofascore import transform, validate
from sofascore.client import FetchError, SofaScoreClient


def resolve_season(client: SofaScoreClient, label: str) -> dict:
    """Turns "2025/26" into the source's identifier for that season."""
    wanted = label.replace("/", "").replace("20", "")  # 2025/26 -> 2526
    for season in client.seasons():
        if season["year"].replace("/", "") == wanted:
            return {"name": label, "source_ref": str(season["id"])}
    available = ", ".join(s["year"] for s in client.seasons())
    raise SystemExit(f"season {label!r} not found. Available: {available}")


def extract_matchday(client: SofaScoreClient, season: dict, matchday: int) -> list[dict]:
    matches = []
    for event in client.round_events(int(season["source_ref"]), matchday):
        if event.get("status", {}).get("type") != "finished":
            print(f"  skipping event {event['id']}: not finished", file=sys.stderr)
            continue
        lineups = client.lineups(event["id"])
        incidents = client.incidents(event["id"])
        match = transform.build_match(event, lineups, incidents)
        validate.validate_match(match, incidents)
        matches.append(match)
    return matches


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--season", required=True, help='e.g. "2025/26"')
    parser.add_argument("--matchday", type=int, required=True)
    parser.add_argument("--through", type=int, help="last matchday of a range")
    parser.add_argument("--out", required=True)
    parser.add_argument("--delay", type=float, default=1.0,
                        help="seconds between requests (default: 1.0)")
    args = parser.parse_args()

    client = SofaScoreClient(delay_seconds=args.delay)
    last = args.through or args.matchday

    try:
        season = resolve_season(client, args.season)
        rounds = []
        for matchday in range(args.matchday, last + 1):
            print(f"matchday {matchday}...", file=sys.stderr)
            matches = extract_matchday(client, season, matchday)
            payload = transform.build_round(
                season, matchday, matches, dt.datetime.now(dt.timezone.utc))
            validate.validate_round(payload)
            rounds.append(payload)
    except validate.ValidationError as exc:
        print(f"REFUSING TO WRITE: {exc}", file=sys.stderr)
        return 2
    except FetchError as exc:
        print(f"COULD NOT FETCH: {exc}", file=sys.stderr)
        return 3

    with open(args.out, "w", encoding="utf-8") as handle:
        json.dump(rounds if args.through else rounds[0], handle,
                  ensure_ascii=False, indent=2)

    performances = sum(len(m["performances"]) for r in rounds for m in r["matches"])
    matches = sum(len(r["matches"]) for r in rounds)
    print(f"wrote {args.out}: {len(rounds)} matchday(s), {matches} matches, "
          f"{performances} performances", file=sys.stderr)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
