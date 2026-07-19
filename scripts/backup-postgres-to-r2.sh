#!/usr/bin/env bash

set -euo pipefail

required_vars=(
  POSTGRES_HOST
  POSTGRES_PORT
  POSTGRES_DB
  POSTGRES_USER
  POSTGRES_PASSWORD
  R2_ENDPOINT
  R2_BUCKET
  R2_ACCESS_KEY
  R2_SECRET_KEY
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "Missing required env var: ${var_name}" >&2
    exit 1
  fi
done

timestamp="$(date -u +%Y-%m-%dT%H-%M-%SZ)"
tmp_dir="$(mktemp -d)"
backup_file="${tmp_dir}/${POSTGRES_DB}-${timestamp}.sql.gz"
backup_prefix="${BACKUP_PREFIX:-postgres}"
daily_retention="${BACKUP_DAILY_RETENTION:-7}"
weekly_retention="${BACKUP_WEEKLY_RETENTION:-4}"
weekly_day="${BACKUP_WEEKLY_DAY_UTC:-7}"
daily_key="s3://${R2_BUCKET}/${backup_prefix}/daily/$(basename "${backup_file}")"
weekly_key="s3://${R2_BUCKET}/${backup_prefix}/weekly/$(basename "${backup_file}")"

cleanup() {
  rm -rf "${tmp_dir}"
}

trap cleanup EXIT

export PGPASSWORD="${POSTGRES_PASSWORD}"
export AWS_ACCESS_KEY_ID="${R2_ACCESS_KEY}"
export AWS_SECRET_ACCESS_KEY="${R2_SECRET_KEY}"
export AWS_EC2_METADATA_DISABLED="true"

pg_dump \
  --host "${POSTGRES_HOST}" \
  --port "${POSTGRES_PORT}" \
  --username "${POSTGRES_USER}" \
  --dbname "${POSTGRES_DB}" \
  --format plain \
  --no-owner \
  --no-privileges \
  | gzip > "${backup_file}"

aws s3 cp \
  "${backup_file}" \
  "${daily_key}" \
  --endpoint-url "${R2_ENDPOINT}"

weekday_utc="$(date -u +%u)"

if [[ "${weekday_utc}" == "${weekly_day}" ]]; then
  aws s3 cp \
    "${backup_file}" \
    "${weekly_key}" \
    --endpoint-url "${R2_ENDPOINT}"

  echo "Weekly backup uploaded to ${weekly_key}"
fi

prune_prefix() {
  local prefix="$1"
  local keep="$2"

  mapfile -t objects < <(
    aws s3 ls "s3://${R2_BUCKET}/${backup_prefix}/${prefix}/" \
      --endpoint-url "${R2_ENDPOINT}" \
      | awk '{print $4}' \
      | sed '/^$/d' \
      | sort
  )

  local total="${#objects[@]}"
  if (( total <= keep )); then
    return
  fi

  local delete_count=$((total - keep))
  for object in "${objects[@]:0:delete_count}"; do
    aws s3 rm \
      "s3://${R2_BUCKET}/${backup_prefix}/${prefix}/${object}" \
      --endpoint-url "${R2_ENDPOINT}"
    echo "Deleted old ${prefix} backup: ${object}"
  done
}

prune_prefix "daily" "${daily_retention}"
prune_prefix "weekly" "${weekly_retention}"

echo "Daily backup uploaded to ${daily_key}"
