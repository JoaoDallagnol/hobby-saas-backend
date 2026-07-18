#!/usr/bin/env bash

set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 /path/to/backup.sql.gz" >&2
  exit 1
fi

backup_file="$1"

required_vars=(
  POSTGRES_HOST
  POSTGRES_PORT
  POSTGRES_DB
  POSTGRES_USER
  POSTGRES_PASSWORD
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "Missing required env var: ${var_name}" >&2
    exit 1
  fi
done

if [[ ! -f "${backup_file}" ]]; then
  echo "Backup file not found: ${backup_file}" >&2
  exit 1
fi

export PGPASSWORD="${POSTGRES_PASSWORD}"

gunzip -c "${backup_file}" | psql \
  --host "${POSTGRES_HOST}" \
  --port "${POSTGRES_PORT}" \
  --username "${POSTGRES_USER}" \
  --dbname "${POSTGRES_DB}"

echo "Restore completed for database ${POSTGRES_DB}"
