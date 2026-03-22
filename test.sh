#!/usr/bin/env bash
set -euo pipefail

INPUT_SERVICE_URL="${INPUT_SERVICE_URL:-http://localhost:8081}"
MATCHING_SERVICE_URL="${MATCHING_SERVICE_URL:-http://localhost:8082}"
MATCHING_LOG_FILE="${MATCHING_LOG_FILE:-logs/events-bets-matching-service.log}"
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

json_equal() {
  local left_json="$1"
  local right_json="$2"
  python3 -c 'import json,sys; print("true" if json.loads(sys.argv[1]) == json.loads(sys.argv[2]) else "false")' "${left_json}" "${right_json}"
}

log_count_published_messages() {
  local event_id="$1"
  local outcome="${2:-}"
  python3 - "${MATCHING_LOG_FILE}" "${event_id}" "${outcome}" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
event_id = sys.argv[2]
outcome = sys.argv[3]

if not path.exists():
    print(0)
    raise SystemExit

needle = f"Published settlement message for eventId={event_id}"
count = 0
with path.open() as handle:
    for line in handle:
        if needle not in line:
            continue
        if outcome and f" outcome={outcome}" not in line:
            continue
        count += 1

print(count)
PY
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
baseline_total=$(log_count_published_messages "${EVENT_ID}")
baseline_won=$(log_count_published_messages "${EVENT_ID}" "WON")
baseline_lost=$(log_count_published_messages "${EVENT_ID}" "LOST")

curl -fsS -X POST "${INPUT_SERVICE_URL}/api/v1/event-outcomes" \
  -H 'Content-Type: application/json' \
  -d "{
    \"eventId\": \"${EVENT_ID}\",
    \"eventName\": \"${EVENT_NAME}\",
    \"eventWinnerId\": \"${EVENT_WINNER_ID}\"
  }" >/dev/null

deadline=$((SECONDS + TIMEOUT_SECONDS))
actual_total=0
actual_won=0
actual_lost=0

while (( SECONDS < deadline )); do
  actual_total=$(( $(log_count_published_messages "${EVENT_ID}") - baseline_total ))
  actual_won=$(( $(log_count_published_messages "${EVENT_ID}" "WON") - baseline_won ))
  actual_lost=$(( $(log_count_published_messages "${EVENT_ID}" "LOST") - baseline_lost ))
  if [[ "${actual_total}" -eq "${expected_total}" && "${actual_won}" -eq "${expected_won}" && "${actual_lost}" -eq "${expected_lost}" ]]; then
    break
  fi
  sleep 2
done

if [[ "${actual_total}" -ne "${expected_total}" ]]; then
  echo "Expected ${expected_total} settlements for ${EVENT_ID}, found ${actual_total}" >&2
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

updated_bets_json=$(curl -fsS "${MATCHING_SERVICE_URL}/api/v1/bets?eventId=${EVENT_ID}")

if [[ "$(json_equal "${initial_bets_json}" "${updated_bets_json}")" != "true" ]]; then
  echo "Expected bets database to remain unchanged after processing ${EVENT_ID}" >&2
  exit 1
fi

echo "Test passed for ${EVENT_ID}: ${actual_total} settlement messages published (${actual_won} WON, ${actual_lost} LOST) and bets DB stayed unchanged."
