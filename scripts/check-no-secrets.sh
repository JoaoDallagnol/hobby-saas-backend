#!/usr/bin/env bash

set -euo pipefail

patterns=(
  'BEGIN PRIVATE KEY'
  'BEGIN RSA PRIVATE KEY'
  'AIza[0-9A-Za-z_-]{20,}'
  'AKIA[0-9A-Z]{16}'
  '-----BEGIN CERTIFICATE-----'
)

ignore_args=(
  --glob=!target/**
  --glob=!.git/**
  --glob=!.idea/**
  --glob=!.vscode/**
  --glob=!*.class
  --glob=!mvnw.cmd
  --glob=!scripts/check-no-secrets.sh
)

status=0

for pattern in "${patterns[@]}"; do
  if rg -n "${ignore_args[@]}" -e "${pattern}" . >/dev/null; then
    echo "Potential secret material found for pattern: ${pattern}" >&2
    rg -n "${ignore_args[@]}" -e "${pattern}" .
    status=1
  fi
done

if [[ "${status}" -ne 0 ]]; then
  exit "${status}"
fi

echo "No obvious committed secret material detected."
