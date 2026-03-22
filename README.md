# Sporty Group Home Assignment

This repository contains a runnable Docker Compose setup for the backend home assignment. It includes:

- `events-input-service`: a Spring Boot API that accepts sports event outcomes over HTTP and publishes them to Kafka topic `event-outcomes` using virtual threads for request handling.
- `events-bets-matching-service`: a Spring Boot service that owns an in-memory H2 bets database, consumes `event-outcomes`, matches bets for the event, and publishes settlement messages to RocketMQ topic `bet-settlements` without mutating the database.
- RocketMQ Dashboard exposed on the host at `http://localhost:8083`

## Requirements Coverage

- Two Spring Boot microservices
- Kafka for event outcome transport
- RocketMQ for bet settlement publishing
- In-memory database populated from the repo-level `sql/` directory at service startup
- Log files written into the repo-level `logs/` directory
- Host-side `test.sh` script that exercises the end-to-end flow

## Run prerequisites

I am a happy owner of MacBook Air M2, and I realized that RocketMQ is very sensitive in terms of the docker host architecture. You need an ARM computer to run the app, any Apple computer with M* processor will work.

Requirements on the host:

- `curl`
- `docker`
- `jq`

## Run

Start the full stack:

```bash
docker compose up --build -d
```

Services exposed on the host:

- `events-input-service`: `http://localhost:8081`
- `events-bets-matching-service`: `http://localhost:8082`
- RocketMQ Dashboard: `http://localhost:8083`
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

The matching service loads sample data from [`sql/data.sql`](sql/data.sql) on startup.

- 100 bets are generated
- 10 events are seeded: `EVT-0001` to `EVT-0010`
- Each event has 10 bets
- For each event, 5 bets back `TEAM-xxxx-A` and 5 bets back `TEAM-xxxx-B`

Example: for `EVT-0001`, sending winner `TEAM-0001-A` should publish 10 settlement messages in total: 5 `WON` and 5 `LOST`.

## Verify Matching And Settlements

Run the host-side smoke test:

```bash
./test.sh
```

The script:

- waits for both services to become healthy
- reads seeded bets for `EVT-0001`
- sends an event outcome to `events-input-service` with a unique default event name for that test run
- consumes settlement messages from RocketMQ in the `rocketmq-broker` container with `./mqadmin consumeMessage -t bet-settlements`
- compares the consumed settlement payloads against the expected `WON` / `LOST` messages and verifies that the bets database remains unchanged

You can also inspect the matching service directly:

```bash
curl "http://localhost:8082/api/v1/bets?eventId=EVT-0001"
```

## Inspect RocketMQ Messages

After producing settlements, inspect the topic from the broker container:

```bash
docker exec -it sporty-group-rocketmq-broker sh
```

Then run:

```bash
cd /home/rocketmq/rocketmq-5.3.2/bin
mqadmin clusterList -n rocketmq-namesrv:9876
mqadmin topicRoute -n rocketmq-namesrv:9876 -t bet-settlements
./mqadmin consumeMessage -n rocketmq-namesrv:9876 -t bet-settlements -s "$(($(date +%s) * 1000 - 120000))" -e "$(($(date +%s) * 1000))" -c 5000
```

The smoke test uses the same `./mqadmin consumeMessage` approach through `docker exec` against `sporty-group-rocketmq-broker`, but it constrains consumption to a buffered time window around the test run. That matters because `consumeMessage` pulls from RocketMQ queue offsets; without `-s` and `-e`, it can return an older slice of the topic instead of the current batch. The script then filters the consumed message bodies for the current run's event id, event name, and winner, and compares them to the expected settlement payloads.

## RocketMQ Dashboard

Once the stack is up, open [http://localhost:8083](http://localhost:8083) in the host browser. The dashboard is configured to talk to `rocketmq-namesrv:9876` inside the Compose network.

## Logs

Both services write logs to the repo-level `logs/` directory:

- [`logs/events-input-service.log`](logs/events-input-service.log)
- [`logs/events-bets-matching-service.log`](logs/events-bets-matching-service.log)

If you delete one of these files while the Java process is still running, the file will not be recreated automatically because the JVM keeps writing to the original open file descriptor. Restart the two Java services from the host to recreate the files:

```bash
docker compose restart events-input-service events-bets-matching-service
```

If you just want to clear the files without restarting the services, truncate them instead of deleting them:

```bash
: > logs/events-input-service.log
: > logs/events-bets-matching-service.log
```
