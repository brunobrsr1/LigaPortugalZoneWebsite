"""Tests for the checks that refuse to produce a payload.

The last test in TestRound is the regression test for the bug that started all of this:
the old scraper lost the expected-goals columns and wrote plausible replacements without
a word. These assertions are the reason that cannot happen again quietly.
"""

import pytest

from sofascore import validate
from sofascore.validate import DataInconsistent, SchemaChanged


def performance(player_ref="100", team_ref="1", goals=0, expected_goals=0.10,
                rating=6.5, minutes_played=90):
    return {
        "player_ref": player_ref, "team_ref": team_ref, "is_starter": True,
        "minutes_played": minutes_played, "goals": goals, "assists": 0,
        "penalties_scored": 0, "yellow_cards": 0, "red_cards": 0,
        "expected_goals": expected_goals, "expected_assists": 0.0, "rating": rating,
    }


def performances(count=22, **overrides):
    return [performance(player_ref=str(i), team_ref="1" if i < count // 2 else "2",
                        **overrides)
            for i in range(count)]


def match(perfs=None, home_score=1, away_score=0):
    return {
        "source_ref": "14131857", "matchday": 1,
        "kickoff_at": "2025-08-08T19:15:00+00:00",
        "home_team": {"source_ref": "1", "name": "Casa Pia"},
        "away_team": {"source_ref": "2", "name": "Sporting CP"},
        "home_score": home_score, "away_score": away_score,
        "players": [],
        "performances": performances() if perfs is None else perfs,
    }


def goal_incident(player_id=100, klass="regular"):
    return {"incidentType": "goal", "incidentClass": klass, "player": {"id": player_id}}


class TestMatch:

    def test_accepts_a_match_whose_three_counts_agree(self):
        scorers = performances()
        scorers[0]["goals"] = 1

        validate.validate_match(match(scorers), [goal_incident()])

    def test_rejects_lineups_and_incidents_that_disagree_on_goals(self):
        # Two independent views of the same event. Guessing which one is right is not
        # this adapter's job; refusing is.
        scorers = performances()
        scorers[0]["goals"] = 2

        with pytest.raises(DataInconsistent, match="lineups credit 2 goals"):
            validate.validate_match(match(scorers), [goal_incident()])

    def test_rejects_a_scoreline_that_the_incidents_do_not_account_for(self):
        scorers = performances()
        scorers[0]["goals"] = 1

        with pytest.raises(DataInconsistent, match="scoreline says 3 goals"):
            validate.validate_match(match(scorers, home_score=3), [goal_incident()])

    def test_rejects_a_performance_for_a_team_that_is_not_playing(self):
        strays = performances()
        strays[0]["team_ref"] = "999"

        with pytest.raises(DataInconsistent, match="neither of the two teams"):
            validate.validate_match(match(strays, home_score=0), [])

    def test_rejects_a_match_with_implausibly_few_players(self):
        with pytest.raises(DataInconsistent, match="produced 4 performances"):
            validate.validate_match(match(performances(4), home_score=0), [])

    def test_rejects_a_match_that_has_not_been_played(self):
        with pytest.raises(DataInconsistent, match="not played yet"):
            validate.validate_match(match(home_score=None), [])


def scoring_match():
    """A 1-0 whose single goal is credited to somebody, which is the coherent case."""
    scorers = performances()
    scorers[0]["goals"] = 1
    return match(scorers)


def round_payload(matches=None):
    return {"source": "sofascore", "extracted_at": "2026-08-19T00:00:00+00:00",
            "season": {"name": "2025/26", "source_ref": "77806"}, "matchday": 1,
            "matches": [scoring_match()] if matches is None else matches}


class TestRound:

    def test_accepts_an_ordinary_matchday(self):
        validate.validate_round(round_payload())

    def test_rejects_a_matchday_with_no_matches(self):
        with pytest.raises(SchemaChanged, match="no matches at all"):
            validate.validate_round(round_payload([]))

    def test_rejects_a_matchday_where_expected_goals_vanished(self):
        # One player without xG is normal. Not one player in the whole matchday having
        # it is the source having stopped sending the field.
        empty = match(performances(expected_goals=None))

        with pytest.raises(SchemaChanged, match="'expected_goals'"):
            validate.validate_round(round_payload([empty]))

    def test_rejects_a_matchday_where_rating_vanished(self):
        with pytest.raises(SchemaChanged, match="'rating'"):
            validate.validate_round(round_payload([match(performances(rating=None))]))

    def test_rejects_a_matchday_where_nobody_played_any_minutes(self):
        with pytest.raises(SchemaChanged, match="minutes played"):
            validate.validate_round(
                round_payload([match(performances(minutes_played=0), home_score=0)]))

    def test_rejects_goals_that_were_scored_but_credited_to_nobody(self):
        # The regression test for the original bug. The scorelines say four goals were
        # scored; no player is credited with any. Read row by row that is 22 players who
        # did not score, which is unremarkable. Read across the matchday it can only mean
        # the `goals` key is gone.
        goalless = [match(performances(goals=0), home_score=3, away_score=1)]

        with pytest.raises(SchemaChanged, match="no player is credited"):
            validate.validate_round(round_payload(goalless))
