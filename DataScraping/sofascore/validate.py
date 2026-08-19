"""Checks that refuse to produce a payload rather than produce a wrong one.

This module exists because of a specific bug. The previous scraper looked for columns
named `Expected_xG` and `xA`, did not find them, and skipped them without a word; pandas
returned two unrelated per-90 columns in their place and the database filled up with
plausible numbers that meant something else entirely. Nobody noticed for a year.

The lesson is not "check the fields exist". Per row that check is impossible here,
because SofaScore omits every zero-valued key, so an absent `goals` is indistinguishable
from a player who did not score. The lesson is that the check belongs at the level of the
whole dataset: one player without a `goals` key is Tuesday, but *nobody in an entire
matchday* having one, while the scorelines say goals were scored, is the API having
changed underneath us.
"""

from __future__ import annotations

from . import transform


class ValidationError(RuntimeError):
    """Base class, so a caller can catch every refusal in one place."""


class SchemaChanged(ValidationError):
    """A field the adapter depends on is missing from the entire dataset."""


class DataInconsistent(ValidationError):
    """The payload contradicts itself, so at least one part of it is wrong."""


# Statistics whose disappearance would otherwise be silent. Counts are excluded on
# purpose: a zero count is legitimate and indistinguishable from an absent key.
NULLABLE_CRITICAL_FIELDS = ("expected_goals", "rating")

MIN_PERFORMANCES_PER_MATCH = 22
MAX_PERFORMANCES_PER_MATCH = 40


def validate_match(match: dict, incidents: list[dict]) -> None:
    """Cross-checks a match against an independent view of the same events.

    The performances are built from the lineups payload and the scoreline comes from the
    fixture, so the goals can be counted three ways from two sources. They have to agree;
    a mismatch means one of them was read wrongly, and guessing which is not our job.
    """
    ref = match["source_ref"]

    if match["home_score"] is None or match["away_score"] is None:
        raise DataInconsistent(f"match {ref} has no score, so it was not played yet")

    count = len(match["performances"])
    if not MIN_PERFORMANCES_PER_MATCH <= count <= MAX_PERFORMANCES_PER_MATCH:
        raise DataInconsistent(
            f"match {ref} produced {count} performances, expected between "
            f"{MIN_PERFORMANCES_PER_MATCH} and {MAX_PERFORMANCES_PER_MATCH}"
        )

    from_lineups = sum(p["goals"] for p in match["performances"])
    from_incidents = sum(transform.goal_tally(incidents).values())
    if from_lineups != from_incidents:
        raise DataInconsistent(
            f"match {ref}: lineups credit {from_lineups} goals but incidents credit "
            f"{from_incidents}"
        )

    scoreline = match["home_score"] + match["away_score"]
    in_incidents = transform.scoreline_goals(incidents)
    if in_incidents != scoreline:
        raise DataInconsistent(
            f"match {ref}: scoreline says {scoreline} goals but incidents contain "
            f"{in_incidents}"
        )

    teams = {match["home_team"]["source_ref"], match["away_team"]["source_ref"]}
    stray = {p["team_ref"] for p in match["performances"]} - teams
    if stray:
        raise DataInconsistent(
            f"match {ref}: performances reference teams {sorted(stray)}, which are "
            f"neither of the two teams playing"
        )


def validate_round(payload: dict) -> None:
    """Dataset-level checks: what a field disappearing looks like from far enough away."""
    matches = payload["matches"]
    if not matches:
        raise SchemaChanged("the matchday contains no matches at all")

    performances = [p for match in matches for p in match["performances"]]
    if not performances:
        raise SchemaChanged("the matchday contains no performances at all")

    for field in NULLABLE_CRITICAL_FIELDS:
        if all(p[field] is None for p in performances):
            raise SchemaChanged(
                f"not one of the {len(performances)} performances carries "
                f"'{field}' — the source has stopped sending it"
            )

    if all(p["minutes_played"] == 0 for p in performances):
        raise SchemaChanged(
            f"not one of the {len(performances)} performances has minutes played"
        )

    # The check the old scraper needed. Goals were scored — the scorelines say so — yet
    # no player is credited with any, which can only mean the key is gone.
    scored = sum(m["home_score"] + m["away_score"] for m in matches)
    credited = sum(p["goals"] for p in performances)
    if scored > 0 and credited == 0:
        raise SchemaChanged(
            f"the scorelines account for {scored} goals but no player is credited with "
            f"any — 'goals' has disappeared from the statistics"
        )
