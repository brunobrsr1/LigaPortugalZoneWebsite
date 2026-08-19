"""Turns SofaScore's payloads into the canonical format, with no network access.

Everything here is a pure function of its arguments, which is what makes it testable
against recorded fixtures instead of against a live API.

The canonical format mirrors the database schema rather than the source: refs are the
source's identifiers, and resolving them to primary keys is the ingestion's job. Nothing
downstream of this module knows the word "SofaScore".
"""

from __future__ import annotations

import datetime as dt
from collections import Counter

OWN_GOAL = "ownGoal"
PENALTY = "penalty"

YELLOW, RED, SECOND_YELLOW = "yellow", "red", "yellowRed"


def build_round(season: dict, matchday: int, matches: list[dict], extracted_at: dt.datetime) -> dict:
    """The whole payload for one matchday."""
    return {
        "source": "sofascore",
        "extracted_at": extracted_at.astimezone(dt.timezone.utc).isoformat(),
        "season": season,
        "matchday": matchday,
        "matches": matches,
    }


def build_match(event: dict, lineups: dict, incidents: list[dict]) -> dict:
    """One match, with a performance row per player who actually played."""
    cards = card_tally(incidents)
    penalties = penalty_tally(incidents)

    home, away = team_identity(event["homeTeam"]), team_identity(event["awayTeam"])

    # The team comes from which side a player is listed under, not from the `teamId` on
    # his lineup entry. That field looks authoritative and is not: in one match the Casa
    # Pia entries carried 36365, 5 and 241802 while Casa Pia is 3011. Whatever it refers
    # to, it is not the club playing. The side is unambiguous.
    performances = []
    for side, team in (("home", home), ("away", away)):
        for entry in lineups.get(side, {}).get("players", []):
            row = performance(entry, cards, penalties, team["source_ref"])
            if row is not None:
                performances.append(row)

    return {
        "source_ref": str(event["id"]),
        "matchday": event["roundInfo"]["round"],
        "kickoff_at": dt.datetime.fromtimestamp(
            event["startTimestamp"], tz=dt.timezone.utc).isoformat(),
        "home_team": home,
        "away_team": away,
        "home_score": event["homeScore"].get("current"),
        "away_score": event["awayScore"].get("current"),
        "players": [player_identity(e["player"]) for e in _played(lineups)],
        "performances": performances,
    }


def performance(entry: dict, cards: dict, penalties: Counter, team_ref: str) -> dict | None:
    """One player's match, or None if he never came on.

    A player who did not play is not a row with zeroes — it is the absence of a row. That
    keeps "appearances" a plain count of rows rather than a count with a condition.

    Absent statistic keys are read as zero, which is correct because SofaScore omits
    zero-valued keys: in one match only 2 of 40 players carried a `goals` key, and they
    were the two scorers. That reading is only safe because `validate` separately checks
    that a key has not vanished from the *entire round*, which is what a schema change
    looks like. Per player it means zero; across every player it means the API changed.
    """
    stats = entry.get("statistics") or {}
    if not _played_at_all(stats):
        return None

    ref = str(entry["player"]["id"])
    return {
        "player_ref": ref,
        "team_ref": team_ref,
        "is_starter": not entry.get("substitute", False),
        # A substitute brought on in stoppage time genuinely has zero minutes, and the
        # key is then omitted like any other zero.
        "minutes_played": int(stats.get("minutesPlayed", 0)),
        "goals": int(stats.get("goals", 0)),
        "assists": int(stats.get("goalAssist", 0)),
        "penalties_scored": penalties.get(ref, 0),
        "yellow_cards": cards.get(ref, {}).get(YELLOW, 0),
        "red_cards": cards.get(ref, {}).get(RED, 0),
        "expected_goals": _decimal(stats.get("expectedGoals")),
        "expected_assists": _decimal(stats.get("expectedAssists")),
        "rating": _decimal(stats.get("rating")),
    }


def player_identity(raw: dict) -> dict:
    return {
        "source_ref": str(raw["id"]),
        "name": raw["name"],
        "country_code": (raw.get("country") or {}).get("alpha3"),
        "born": _date(raw.get("dateOfBirthTimestamp")),
        "position": raw.get("position"),
    }


def team_identity(raw: dict) -> dict:
    return {"source_ref": str(raw["id"]), "name": raw["name"]}


def card_tally(incidents: list[dict]) -> dict[str, dict[str, int]]:
    """Cards are not in the lineups payload at all; they only exist as incidents.

    A second yellow counts as both: the player received a yellow and was sent off.
    """
    tally: dict[str, dict[str, int]] = {}
    for incident in incidents:
        if incident.get("incidentType") != "card":
            continue
        player = incident.get("player")
        if not player:
            continue  # a card shown to the bench carries no player
        counts = tally.setdefault(str(player["id"]), {YELLOW: 0, RED: 0})
        klass = incident.get("incidentClass")
        if klass == YELLOW:
            counts[YELLOW] += 1
        elif klass == RED:
            counts[RED] += 1
        elif klass == SECOND_YELLOW:
            counts[YELLOW] += 1
            counts[RED] += 1
    return tally


def penalty_tally(incidents: list[dict]) -> Counter:
    """Scored penalties. The lineups payload cannot distinguish these from open play."""
    return Counter(
        str(incident["player"]["id"])
        for incident in incidents
        if incident.get("incidentType") == "goal"
        and incident.get("incidentClass") == PENALTY
        and incident.get("player")
    )


def goal_tally(incidents: list[dict]) -> Counter:
    """Goals credited to the scorer. Own goals change the scoreline but are not his."""
    return Counter(
        str(incident["player"]["id"])
        for incident in incidents
        if incident.get("incidentType") == "goal"
        and incident.get("incidentClass") != OWN_GOAL
        and incident.get("player")
    )


def scoreline_goals(incidents: list[dict]) -> int:
    """Every goal, own goals included, which is what the scoreboard shows."""
    return sum(1 for i in incidents if i.get("incidentType") == "goal")


def _played(lineups: dict) -> list[dict]:
    return [
        entry
        for side in ("home", "away")
        for entry in lineups.get(side, {}).get("players", [])
        if _played_at_all(entry.get("statistics") or {})
    ]


def _played_at_all(stats: dict) -> bool:
    """An unused substitute carries two bookkeeping keys and nothing else.

    `rating` is the reliable signal because it is never zero and so is never omitted;
    `minutesPlayed` alone would misclassify a stoppage-time substitute.
    """
    return "rating" in stats or "minutesPlayed" in stats


def _decimal(value) -> float | None:
    return None if value is None else round(float(value), 2)


def _date(timestamp) -> str | None:
    if not timestamp:
        return None
    return dt.datetime.fromtimestamp(timestamp, tz=dt.timezone.utc).date().isoformat()
