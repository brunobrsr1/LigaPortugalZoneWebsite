CREATE TABLE IF NOT EXISTS player_data (
    player_name VARCHAR(100),
    nation VARCHAR(50),
    team_name VARCHAR(100),
    position VARCHAR(50),
    age INTEGER,
    matches_played INTEGER,
    starts INTEGER,
    minutes_played INTEGER,
    goals INTEGER,
    assists INTEGER,
    penalties_scored INTEGER,
    yellow_cards INTEGER,
    red_cards INTEGER,
    expected_goals FLOAT,
    expected_assists FLOAT
);

COPY player_data FROM '/docker-entrypoint-initdb.d/players_primeira_liga.csv' DELIMITER ',' CSV HEADER;