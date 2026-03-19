INSERT INTO bets (
    bet_id,
    user_id,
    event_id,
    event_market_id,
    event_winner_id,
    bet_amount,
    settled,
    settlement_status,
    settled_at,
    settlement_message_id
)
SELECT
    generated.sequence_id,
    'USER-' || RIGHT('0000' || CAST(generated.sequence_id AS VARCHAR), 4),
    'EVT-' || RIGHT('0000' || CAST(generated.event_number AS VARCHAR), 4),
    'WINNER-MARKET-' || RIGHT('0000' || CAST(generated.event_number AS VARCHAR), 4),
    CASE
        WHEN generated.event_bet_index < 5
            THEN 'TEAM-' || RIGHT('0000' || CAST(generated.event_number AS VARCHAR), 4) || '-A'
        ELSE 'TEAM-' || RIGHT('0000' || CAST(generated.event_number AS VARCHAR), 4) || '-B'
    END,
    CAST(25 + (generated.event_bet_index * 5) AS DECIMAL(10, 2)),
    FALSE,
    NULL,
    NULL,
    NULL
FROM (
    SELECT
        x AS sequence_id,
        CAST(((x - 1) / 10) AS INTEGER) + 1 AS event_number,
        MOD(x - 1, 10) AS event_bet_index
    FROM SYSTEM_RANGE(1, 100)
) generated;

