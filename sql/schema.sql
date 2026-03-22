DROP TABLE IF EXISTS bets;

CREATE TABLE bets (
    bet_id BIGINT PRIMARY KEY,
    user_id VARCHAR(32) NOT NULL,
    event_id VARCHAR(32) NOT NULL,
    event_market_id VARCHAR(32) NOT NULL,
    event_winner_id VARCHAR(32) NOT NULL,
    bet_amount DECIMAL(10, 2) NOT NULL
);

CREATE INDEX idx_bets_event_id ON bets (event_id);
