"""Unit tests for the pure half of the adapter. No network, no fixtures on disk."""

import pytest

from sofascore import transform


def lineup_entry(player_id="100", substitute=False, **stats):
    return {
        "player": {"id": int(player_id), "name": "Someone", "position": "M",
                   "country": {"alpha3": "PRT"}, "dateOfBirthTimestamp": 920246400},
        "substitute": substitute,
        "statistics": stats,
    }


class TestPerformance:

    def test_an_unused_substitute_produces_no_row_at_all(self):
        # Two bookkeeping keys and nothing else is what the bench looks like. The absence
        # of a row is what keeps "appearances" a plain count.
        bench = lineup_entry(substitute=True, ratingVersions={}, statisticsType="overall")

        assert transform.performance(bench, {}, {}, "1") is None

    def test_reads_a_starter(self):
        entry = lineup_entry(minutesPlayed=90, rating=7.6, goals=1, goalAssist=2,
                             expectedGoals=0.3149, expectedAssists=0.05)

        row = transform.performance(entry, {}, {}, "1")

        assert row["is_starter"] is True
        assert row["minutes_played"] == 90
        assert row["goals"] == 1
        assert row["assists"] == 2
        assert row["expected_goals"] == 0.31
        assert row["rating"] == 7.6

    def test_absent_statistics_are_read_as_zero(self):
        # SofaScore omits every zero-valued key: in one real match only 2 of 40 players
        # carried a `goals` key. Per player, absent means zero. That a key has not
        # vanished from the whole dataset is validate's job, not this function's.
        entry = lineup_entry(minutesPlayed=61, rating=6.4)

        row = transform.performance(entry, {}, {}, "1")

        assert row["goals"] == 0
        assert row["assists"] == 0
        assert row["expected_goals"] is None

    def test_counts_a_stoppage_time_substitute_who_has_no_minutes(self):
        # Zero minutes is a zero, so the key is omitted — but a rating is never zero and
        # so is never omitted. Keying off minutes alone would drop this appearance.
        entry = lineup_entry(substitute=True, rating=6.0)

        row = transform.performance(entry, {}, {}, "1")

        assert row is not None
        assert row["is_starter"] is False
        assert row["minutes_played"] == 0

    def test_takes_cards_and_penalties_from_the_incident_tallies(self):
        entry = lineup_entry(player_id="7", minutesPlayed=90, rating=7.0, goals=1)

        row = transform.performance(entry, {"7": {"yellow": 2, "red": 1}}, {"7": 1}, "1")

        assert row["yellow_cards"] == 2
        assert row["red_cards"] == 1
        assert row["penalties_scored"] == 1


class TestIncidentTallies:

    def test_a_second_yellow_counts_as_both_a_yellow_and_a_red(self):
        incidents = [{"incidentType": "card", "incidentClass": "yellowRed",
                      "player": {"id": 7}}]

        assert transform.card_tally(incidents) == {"7": {"yellow": 1, "red": 1}}

    def test_ignores_a_card_shown_to_the_bench(self):
        # Incidents without a player exist; they must not become a KeyError.
        assert transform.card_tally([{"incidentType": "card", "incidentClass": "red"}]) == {}

    def test_own_goals_are_not_credited_to_the_scorer(self):
        incidents = [
            {"incidentType": "goal", "incidentClass": "regular", "player": {"id": 1}},
            {"incidentType": "goal", "incidentClass": "ownGoal", "player": {"id": 2}},
        ]

        assert transform.goal_tally(incidents) == {"1": 1}

    def test_but_own_goals_do_count_on_the_scoreboard(self):
        incidents = [
            {"incidentType": "goal", "incidentClass": "regular", "player": {"id": 1}},
            {"incidentType": "goal", "incidentClass": "ownGoal", "player": {"id": 2}},
        ]

        assert transform.scoreline_goals(incidents) == 2

    def test_penalties_are_a_subset_of_goals(self):
        incidents = [
            {"incidentType": "goal", "incidentClass": "penalty", "player": {"id": 1}},
            {"incidentType": "goal", "incidentClass": "regular", "player": {"id": 1}},
        ]

        assert transform.penalty_tally(incidents) == {"1": 1}
        assert transform.goal_tally(incidents) == {"1": 2}


class TestPlayerIdentity:

    def test_stores_the_birth_date_rather_than_an_age(self):
        raw = {"id": 42, "name": "Patrick Sequeira", "position": "G",
               "country": {"alpha3": "CRI"}, "dateOfBirthTimestamp": 920246400}

        assert transform.player_identity(raw) == {
            "source_ref": "42",
            "name": "Patrick Sequeira",
            "country_code": "CRI",
            "born": "1999-03-01",
            "position": "G",
        }

    def test_survives_a_player_with_no_country(self):
        raw = {"id": 42, "name": "Someone", "position": "M"}

        identity = transform.player_identity(raw)

        assert identity["country_code"] is None
        assert identity["born"] is None
