#!/bin/sh
set -eu

until mc alias set local http://minio:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"; do
  sleep 2
done
mc mb --ignore-existing local/hrcore-documents-dev
if ! mc admin user info local "$MINIO_APP_USER" >/dev/null 2>&1; then
  mc admin user add local "$MINIO_APP_USER" "$MINIO_APP_PASSWORD"
fi
if ! mc admin policy info local hrcore-documents-dev >/dev/null 2>&1; then
  mc admin policy create local hrcore-documents-dev /policy/minio-app-policy.json
fi
mc admin policy attach local hrcore-documents-dev --user "$MINIO_APP_USER"
