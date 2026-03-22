#!/usr/bin/env bash
set -euo pipefail

INPUT_SERVICE_URL="${INPUT_SERVICE_URL:-http://localhost:8081}"
MATCHING_SERVICE_URL="${MATCHING_SERVICE_URL:-http://localhost:8082}"
EVENT_ID="${EVENT_ID:-EVT-0001}"
DEFAULT_EVENT_NAME="Championship Match ${EVENT_ID} test-$(date -u +%Y%m%dT%H%M%S)"
EVENT_NAME="${EVENT_NAME:-${DEFAULT_EVENT_NAME}}"
EVENT_WINNER_ID="${EVENT_WINNER_ID:-TEAM-0001-A}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-30}"
HTTP_MAX_TIME_SECONDS="${HTTP_MAX_TIME_SECONDS:-5}"
ROCKETMQ_BROKER_CONTAINER="${ROCKETMQ_BROKER_CONTAINER:-sporty-group-rocketmq-broker}"
ROCKETMQ_NAMESRV_ADDR="${ROCKETMQ_NAMESRV_ADDR:-rocketmq-namesrv:9876}"
ROCKETMQ_TOPIC="${ROCKETMQ_TOPIC:-bet-settlements}"
MQADMIN_COMMAND="${MQADMIN_COMMAND:-./mqadmin}"
CONSUME_MESSAGE_COUNT="${CONSUME_MESSAGE_COUNT:-5000}"
CONSUME_LOOKBACK_MILLISECONDS="${CONSUME_LOOKBACK_MILLISECONDS:-120000}"

require_command() {
  local command_name="$1"
  if ! command -v "${command_name}" >/dev/null 2>&1; then
    echo "Required command not found: ${command_name}" >&2
    exit 1
  fi
}

ensure_container_running() {
  local container_name="$1"
  local is_running

  is_running=$(docker inspect -f '{{.State.Running}}' "${container_name}" 2>/dev/null || true)
  if [[ "${is_running}" != "true" ]]; then
    echo "Required Docker container is not running: ${container_name}" >&2
    exit 1
  fi
}

docker_exec_broker() {
  if [[ -t 0 && -t 1 ]]; then
    docker exec -it "$@"
  else
    docker exec -i "$@"
  fi
}

wait_for_health() {
  local service_name="$1"
  local service_url="$2"
  local deadline=$((SECONDS + TIMEOUT_SECONDS))

  while (( SECONDS < deadline )); do
    local status
    status=$(curl --max-time "${HTTP_MAX_TIME_SECONDS}" -fsS "${service_url}/actuator/health" 2>/dev/null | jq -r '.status' 2>/dev/null || true)
    if [[ "${status}" == "UP" ]]; then
      return 0
    fi
    sleep 2
  done

  echo "Timed out waiting for ${service_name} at ${service_url}" >&2
  return 1
}

json_count() {
  jq 'length'
}

json_count_matching_winner() {
  local winner_id="$1"
  jq --arg winner_id "${winner_id}" '[.[] | select(.eventWinnerId == $winner_id)] | length'
}

json_equal() {
  local left_json="$1"
  local right_json="$2"
  if [[ "$(printf '%s' "${left_json}" | jq -S -c .)" == "$(printf '%s' "${right_json}" | jq -S -c .)" ]]; then
    echo "true"
  else
    echo "false"
  fi
}

consume_settlement_bodies() {
  local begin_timestamp="$1"
  local end_timestamp="$2"
  local message_count="$3"
  docker_exec_broker \
    -e CONSUME_BEGIN_TIMESTAMP="${begin_timestamp}" \
    -e CONSUME_END_TIMESTAMP="${end_timestamp}" \
    -e CONSUME_MESSAGE_COUNT="${message_count}" \
    -e MQADMIN_COMMAND="${MQADMIN_COMMAND}" \
    -e ROCKETMQ_NAMESRV_ADDR="${ROCKETMQ_NAMESRV_ADDR}" \
    -e ROCKETMQ_TOPIC="${ROCKETMQ_TOPIC}" \
    "${ROCKETMQ_BROKER_CONTAINER}" \
    sh -lc '
      cd /home/rocketmq/rocketmq-5.3.2/bin &&
      "$MQADMIN_COMMAND" consumeMessage \
        -n "$ROCKETMQ_NAMESRV_ADDR" \
        -t "$ROCKETMQ_TOPIC" \
        -s "$CONSUME_BEGIN_TIMESTAMP" \
        -e "$CONSUME_END_TIMESTAMP" \
        -c "$CONSUME_MESSAGE_COUNT"
    ' 2>/dev/null | tr -d '\r' | sed -n 's/^.* BODY: //p'
}

build_expected_settlements_json() {
  local bets_json="$1"
  printf '%s' "${bets_json}" | jq \
    --arg event_name "${EVENT_NAME}" \
    --arg actual_winner_id "${EVENT_WINNER_ID}" \
    '
      map({
        betId,
        userId,
        eventId,
        eventName: $event_name,
        eventMarketId,
        selectedWinnerId: .eventWinnerId,
        actualWinnerId: $actual_winner_id,
        settlementOutcome: (
          if .eventWinnerId == $actual_winner_id
          then "WON"
          else "LOST"
          end
        ),
        betAmount
      })
      | sort_by(.betId)
    '
}

normalize_consumed_settlements_json() {
  jq -s \
    --arg event_id "${EVENT_ID}" \
    --arg event_name "${EVENT_NAME}" \
    --arg actual_winner_id "${EVENT_WINNER_ID}" \
    '
      map(
        select(
          .eventId == $event_id
          and .eventName == $event_name
          and .actualWinnerId == $actual_winner_id
        )
        | {
            betId,
            userId,
            eventId,
            eventName,
            eventMarketId,
            selectedWinnerId,
            actualWinnerId,
            settlementOutcome,
            betAmount
          }
      )
      | sort_by(.betId)
    '
}

count_settled_at_values() {
  jq -s \
    --arg event_id "${EVENT_ID}" \
    --arg event_name "${EVENT_NAME}" \
    --arg actual_winner_id "${EVENT_WINNER_ID}" \
    '
      [
        .[]
        | select(
            .eventId == $event_id
            and .eventName == $event_name
            and .actualWinnerId == $actual_winner_id
            and (.settledAt // "") != ""
          )
      ]
      | length
    '
}

require_command curl
require_command docker
require_command jq
ensure_container_running "${ROCKETMQ_BROKER_CONTAINER}"

wait_for_health "events-input-service" "${INPUT_SERVICE_URL}"
wait_for_health "events-bets-matching-service" "${MATCHING_SERVICE_URL}"

initial_bets_json=$(curl --max-time "${HTTP_MAX_TIME_SECONDS}" -fsS "${MATCHING_SERVICE_URL}/api/v1/bets?eventId=${EVENT_ID}")
expected_settlements_json=$(build_expected_settlements_json "${initial_bets_json}")
expected_total=$(printf '%s' "${initial_bets_json}" | json_count)
expected_won=$(printf '%s' "${initial_bets_json}" | json_count_matching_winner "${EVENT_WINNER_ID}")
expected_lost=$((expected_total - expected_won))

if [[ "${expected_total}" -eq 0 ]]; then
  echo "No seeded bets found for event ${EVENT_ID}" >&2
  exit 1
fi

echo "Sending event outcome for ${EVENT_ID} with winner ${EVENT_WINNER_ID}"
post_response_file=$(mktemp)
post_status_code=""
consume_begin_timestamp=$(( $(date +%s) * 1000 - CONSUME_LOOKBACK_MILLISECONDS ))

cleanup() {
  rm -f "${post_response_file}"
}

trap cleanup EXIT

if ! post_status_code=$(curl \
  --http1.1 \
  --max-time "${HTTP_MAX_TIME_SECONDS}" \
  --silent \
  --show-error \
  --output "${post_response_file}" \
  --write-out '%{http_code}' \
  -X POST "${INPUT_SERVICE_URL}/api/v1/event-outcomes" \
  -H 'Connection: close' \
  -H 'Content-Type: application/json' \
  -d "{
    \"eventId\": \"${EVENT_ID}\",
    \"eventName\": \"${EVENT_NAME}\",
    \"eventWinnerId\": \"${EVENT_WINNER_ID}\"
  }"); then
  echo "Failed to call events-input-service POST /api/v1/event-outcomes" >&2
  exit 1
fi

if [[ "${post_status_code}" != "202" ]]; then
  echo "Expected HTTP 202 from events-input-service, got ${post_status_code}" >&2
  if [[ -s "${post_response_file}" ]]; then
    echo "Response body:" >&2
    cat "${post_response_file}" >&2
  fi
  exit 1
fi

deadline=$((SECONDS + TIMEOUT_SECONDS))
actual_total=0
actual_won=0
actual_lost=0
actual_settlements_json='[]'
actual_settled_at_count=0
actual_bet_ids=''

while (( SECONDS < deadline )); do
  consume_end_timestamp=$(( $(date +%s) * 1000 ))
  consumed_bodies=$(consume_settlement_bodies "${consume_begin_timestamp}" "${consume_end_timestamp}" "${CONSUME_MESSAGE_COUNT}" || true)
  if [[ -n "${consumed_bodies}" ]]; then
    actual_settlements_json=$(printf '%s\n' "${consumed_bodies}" | normalize_consumed_settlements_json)
    actual_settled_at_count=$(printf '%s\n' "${consumed_bodies}" | count_settled_at_values)
  else
    actual_settlements_json='[]'
    actual_settled_at_count=0
  fi
  actual_total=$(printf '%s' "${actual_settlements_json}" | jq 'length')
  actual_won=$(printf '%s' "${actual_settlements_json}" | jq '[.[] | select(.settlementOutcome == "WON")] | length')
  actual_lost=$(printf '%s' "${actual_settlements_json}" | jq '[.[] | select(.settlementOutcome == "LOST")] | length')
  actual_bet_ids=$(printf '%s' "${actual_settlements_json}" | jq -r 'map(.betId) | sort | join(",")')
  if [[ \
    "${actual_total}" -eq "${expected_total}" && \
    "${actual_won}" -eq "${expected_won}" && \
    "${actual_lost}" -eq "${expected_lost}" && \
    "$(json_equal "${expected_settlements_json}" "${actual_settlements_json}")" == "true" && \
    "${actual_settled_at_count}" -eq "${expected_total}" \
  ]]; then
    break
  fi
  sleep 2
done

if [[ "${actual_total}" -ne "${expected_total}" ]]; then
  echo "Expected ${expected_total} settlements for ${EVENT_ID}, found ${actual_total}. Consumed betIds=[${actual_bet_ids}]" >&2
  exit 1
fi

if [[ "${actual_won}" -ne "${expected_won}" ]]; then
  echo "Expected ${expected_won} WON settlement messages, found ${actual_won}" >&2
  exit 1
fi

if [[ "${actual_lost}" -ne "${expected_lost}" ]]; then
  echo "Expected ${expected_lost} LOST settlement messages, found ${actual_lost}" >&2
  exit 1
fi

if [[ "$(json_equal "${expected_settlements_json}" "${actual_settlements_json}")" != "true" ]]; then
  echo "Consumed settlement messages do not match the expected payloads for ${EVENT_ID}" >&2
  exit 1
fi

if [[ "${actual_settled_at_count}" -ne "${expected_total}" ]]; then
  echo "Expected ${expected_total} settlement messages with settledAt, found ${actual_settled_at_count}" >&2
  exit 1
fi

updated_bets_json=$(curl --max-time "${HTTP_MAX_TIME_SECONDS}" -fsS "${MATCHING_SERVICE_URL}/api/v1/bets?eventId=${EVENT_ID}")

if [[ "$(json_equal "${initial_bets_json}" "${updated_bets_json}")" != "true" ]]; then
  echo "Expected bets database to remain unchanged after processing ${EVENT_ID}" >&2
  exit 1
fi

echo "Test passed for ${EVENT_ID}: ${actual_total} settlement messages published (${actual_won} WON, ${actual_lost} LOST) and bets DB stayed unchanged."
