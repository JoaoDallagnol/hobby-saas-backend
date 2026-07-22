#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
POSTMAN_DIR="${ROOT_DIR}/postman"
COMPOSE_FILE="${POSTMAN_DIR}/docker-compose.acceptance.yml"
RESULT_DIR="${ROOT_DIR}/target/postman"
FREE_TOKEN="acceptance-free-token"
PLUS_TOKEN="acceptance-plus-token"
MODE="run"
KEEP_STACK="false"

for argument in "$@"; do
  case "${argument}" in
    --prepare-only) MODE="prepare"; KEEP_STACK="true" ;;
    --keep) KEEP_STACK="true" ;;
    *) echo "Unknown argument: ${argument}" >&2; exit 2 ;;
  esac
done

compose() {
  docker compose -f "${COMPOSE_FILE}" "$@"
}

reset_stack() {
  if compose down -v --remove-orphans; then
    return
  fi

  echo "Docker daemon could not stop the isolated containers; terminating their local PID 1 processes and retrying." >&2
  for service in app s3mock postgres; do
    compose exec -T "${service}" kill -TERM 1 >/dev/null 2>&1 || true
  done
  for _ in $(seq 1 20); do
    if [[ -z "$(compose ps --status running --services)" ]]; then
      break
    fi
    sleep 1
  done
  compose down -v --remove-orphans
}

show_diagnostics() {
  compose ps || true
  compose logs --tail=200 app postgres s3mock || true
}

on_error() {
  status=$?
  echo "Acceptance setup or execution failed. Collecting diagnostics..." >&2
  show_diagnostics
  exit "${status}"
}

trap on_error ERR

reset_stack
compose up -d --build

for attempt in $(seq 1 60); do
  if curl --fail --silent --show-error http://127.0.0.1:18080/actuator/health >/dev/null; then
    break
  fi
  if [[ "${attempt}" == "60" ]]; then
    echo "API did not become healthy in time." >&2
    exit 1
  fi
  sleep 2
done

curl --fail --silent --show-error \
  -H "Authorization: Bearer ${FREE_TOKEN}" \
  http://127.0.0.1:18080/api/me >/dev/null
curl --fail --silent --show-error \
  -H "Authorization: Bearer ${PLUS_TOKEN}" \
  http://127.0.0.1:18080/api/me >/dev/null

compose exec -T postgres psql \
  --username acceptance \
  --dbname hobby_saas_acceptance \
  --set ON_ERROR_STOP=1 \
  --command "INSERT INTO subscriptions (user_id, plan, status, provider, current_period_end, updated_at) VALUES ('acceptance-plus-user', 'plus', 'active', 'acceptance-fixture', now() + interval '30 days', now()) ON CONFLICT (user_id) DO UPDATE SET plan = EXCLUDED.plan, status = EXCLUDED.status, provider = EXCLUDED.provider, current_period_end = EXCLUDED.current_period_end, updated_at = EXCLUDED.updated_at;" >/dev/null

if [[ "${MODE}" == "prepare" ]]; then
  trap - ERR
  echo "Acceptance stack prepared at http://127.0.0.1:18080 and left running."
  echo "Import the files from ${POSTMAN_DIR} into Postman and run the collection."
  exit 0
fi

if [[ ! -x "${POSTMAN_DIR}/node_modules/.bin/postman" ]]; then
  npm --prefix "${POSTMAN_DIR}" ci
fi

mkdir -p "${RESULT_DIR}" "${RESULT_DIR}/home"
HOME="${RESULT_DIR}/home" "${POSTMAN_DIR}/node_modules/.bin/postman" collection run \
  "${POSTMAN_DIR}/HobbySaaS.postman_collection.json" \
  -e "${POSTMAN_DIR}/local.postman_environment.json" \
  --reporters cli,json,junit \
  --reporter-json-export "${RESULT_DIR}/postman-results.json" \
  --reporter-junit-export "${RESULT_DIR}/postman-results.xml"

compose ps
if compose logs app postgres s3mock | grep -E "(^|[[:space:]])(ERROR|FATAL)([[:space:]]|$)"; then
  echo "Unexpected ERROR/FATAL entry found in acceptance logs." >&2
  exit 1
fi

trap - ERR
if [[ "${KEEP_STACK}" == "true" ]]; then
  echo "Acceptance suite passed; stack kept at http://127.0.0.1:18080."
else
  reset_stack
  echo "Acceptance suite passed; isolated stack and volumes removed."
fi
