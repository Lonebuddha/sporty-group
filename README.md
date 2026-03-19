# Sporty Group Home Assignment

This repository contains a runnable Docker Compose setup for the backend home assignment. It includes:

- `events-input-service`: a Spring Boot API that accepts sports event outcomes over HTTP and publishes them to Kafka topic `event-outcomes` using virtual threads for request handling.
- `events-bets-matching-service`: a Spring Boot service that owns an in-memory H2 bets database, consumes `event-outcomes`, matches unsettled bets for the event, and publishes settlement messages to RocketMQ topic `bet-settlements`.

## Requirements Coverage

- Two Spring Boot microservices
- Kafka for event outcome transport
- RocketMQ for bet settlement publishing
- In-memory database populated from the repo-level `sql/` directory at service startup
- Log files written into the repo-level `logs/` directory
- Host-side `test.sh` script that exercises the end-to-end flow

## Run

Start the full stack:

```bash
docker compose up --build -d
```

Services exposed on the host:

- `events-input-service`: `http://localhost:8081`
- `events-bets-matching-service`: `http://localhost:8082`
- RocketMQ name server: `localhost:9876`

You can check health with:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

## Send An Event Outcome

Example `curl` call from the host machine:

```bash
curl -X POST http://localhost:8081/api/v1/event-outcomes \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId": "EVT-0001",
    "eventName": "Championship Match EVT-0001",
    "eventWinnerId": "TEAM-0001-A"
  }'
```

## Seed Data

The matching service loads sample data from [`sql/data.sql`](/Users/lonebuddha/sporty-group/git/sporty-group/sql/data.sql) on startup.

- 100 bets are generated
- 10 events are seeded: `EVT-0001` to `EVT-0010`
- Each event has 10 bets
- For each event, 5 bets back `TEAM-xxxx-A` and 5 bets back `TEAM-xxxx-B`

Example: for `EVT-0001`, sending winner `TEAM-0001-A` should produce 10 settlements in total: 5 `WON` and 5 `LOST`.

## Verify Matching And Settlements

Run the host-side smoke test:

```bash
./test.sh
```

The script:

- waits for both services to become healthy
- reads seeded bets for `EVT-0001`
- sends an event outcome to `events-input-service`
- polls `events-bets-matching-service`
- asserts that all matching bets are settled and that the `WON` / `LOST` counts are correct

You can also inspect the matching service directly:

```bash
curl "http://localhost:8082/api/v1/bets?eventId=EVT-0001"
curl "http://localhost:8082/api/v1/settlements?eventId=EVT-0001"
```

## Inspect RocketMQ Messages

After producing settlements, inspect the topic with `mqadmin` from inside the broker container:

```bash
docker compose exec rocketmq-broker sh
```

Then run:

```bash
mqadmin clusterList -n rocketmq-namesrv:9876
mqadmin topicRoute -n rocketmq-namesrv:9876 -t bet-settlements
```

## Logs

Both services write logs to the repo-level `logs/` directory:

- [`logs/events-input-service.log`](/Users/lonebuddha/sporty-group/git/sporty-group/logs/events-input-service.log)
- [`logs/events-bets-matching-service.log`](/Users/lonebuddha/sporty-group/git/sporty-group/logs/events-bets-matching-service.log)
