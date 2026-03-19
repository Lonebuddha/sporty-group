#!/usr/bin/env bash
set -euo pipefail

INPUT_SERVICE_URL="${INPUT_SERVICE_URL:-http://localhost:8081}"
MATCHING_SERVICE_URL="${MATCHING_SERVICE_URL:-http://localhost:8082}"
EVENT_ID="${EVENT_ID:-EVT-0001}"
EVENT_NAME="${EVENT_NAME:-Championship Match EVT-0001}"
EVENT_WINNER_ID="${EVENT_WINNER_ID:-TEAM-0001-A}"
TIMEOUT_SECONDS="${TIMEOUT_SECONDS:-60}"

wait_for_health() {
  local service_name="$1"
  local service_url="$2"
  local deadline=$((SECONDS + TIMEOUT_SECONDS))

  while (( SECONDS < deadline )); do
    local status
    status=$(curl -fsS "${service_url}/actuator/health" 2>/dev/null | python3 -c 'import json,sys; print(json.load(sys.stdin)["status"])' 2>/dev/null || true)
    if [[ "${status}" == "UP" ]]; then
      return 0
    fi
    sleep 2
  done

  echo "Timed out waiting for ${service_name} at ${service_url}" >&2
  return 1
}

json_count() {
  python3 -c 'import json,sys; print(len(json.load(sys.stdin)))'
}

json_count_matching_winner() {
  local winner_id="$1"
  python3 -c 'import json,sys; winner_id=sys.argv[1]; data=json.load(sys.stdin); print(sum(1 for item in data if item["eventWinnerId"] == winner_id))' "${winner_id}"
}

json_count_outcome() {
  local outcome="$1"
  python3 -c 'import json,sys; outcome=sys.argv[1]; data=json.load(sys.stdin); print(sum(1 for item in data if item["settlementOutcome"] == outcome))' "${outcome}"
}

json_count_settled_bets() {
  python3 -c 'import json,sys; data=json.load(sys.stdin); print(sum(1 for item in data if item["settled"]))'
}

wait_for_health "events-input-service" "${INPUT_SERVICE_URL}"
wait_for_health "events-bets-matching-service" "${MATCHING_SERVICE_URL}"

initial_bets_json=$(curl -fsS "${MATCHING_SERVICE_URL}/api/v1/bets?eventId=${EVENT_ID}")
expected_total=$(printf '%s' "${initial_bets_json}" | json_count)
expected_won=$(printf '%s' "${initial_bets_json}" | json_count_matching_winner "${EVENT_WINNER_ID}")
expected_lost=$((expected_total - expected_won))

if [[ "${expected_total}" -eq 0 ]]; then
  echo "No seeded bets found for event ${EVENT_ID}" >&2
  exit 1
fi

echo "Sending event outcome for ${EVENT_ID} with winner ${EVENT_WINNER_ID}"
curl -fsS -X POST "${INPUT_SERVICE_URL}/api/v1/event-outcomes" \
  -H 'Content-Type: application/json' \
  -d "{
    \"eventId\": \"${EVENT_ID}\",
    \"eventName\": \"${EVENT_NAME}\",
    \"eventWinnerId\": \"${EVENT_WINNER_ID}\"
  }" >/dev/null

deadline=$((SECONDS + TIMEOUT_SECONDS))
actual_total=0
settlements_json='[]'

while (( SECONDS < deadline )); do
  settlements_json=$(curl -fsS "${MATCHING_SERVICE_URL}/api/v1/settlements?eventId=${EVENT_ID}")
  actual_total=$(printf '%s' "${settlements_json}" | json_count)
  if [[ "${actual_total}" -eq "${expected_total}" ]]; then
    break
  fi
  sleep 2
done

if [[ "${actual_total}" -ne "${expected_total}" ]]; then
  echo "Expected ${expected_total} settlements for ${EVENT_ID}, found ${actual_total}" >&2
  exit 1
fi

actual_won=$(printf '%s' "${settlements_json}" | json_count_outcome "WON")
actual_lost=$(printf '%s' "${settlements_json}" | json_count_outcome "LOST")

if [[ "${actual_won}" -ne "${expected_won}" ]]; then
  echo "Expected ${expected_won} WON settlements, found ${actual_won}" >&2
  exit 1
fi

if [[ "${actual_lost}" -ne "${expected_lost}" ]]; then
  echo "Expected ${expected_lost} LOST settlements, found ${actual_lost}" >&2
  exit 1
fi

updated_bets_json=$(curl -fsS "${MATCHING_SERVICE_URL}/api/v1/bets?eventId=${EVENT_ID}")
settled_bets=$(printf '%s' "${updated_bets_json}" | json_count_settled_bets)

if [[ "${settled_bets}" -ne "${expected_total}" ]]; then
  echo "Expected ${expected_total} bets to be marked as settled, found ${settled_bets}" >&2
  exit 1
fi

echo "Test passed for ${EVENT_ID}: ${actual_total} settlements created (${actual_won} WON, ${actual_lost} LOST)."
