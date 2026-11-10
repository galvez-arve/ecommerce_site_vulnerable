#!/bin/bash
# Restores the original dataset, then layers on the vulnerable-lab schema changes.
set -e
echo ">>> Restoring restorepoint.sql into $POSTGRES_DB"
pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-privileges /docker-entrypoint-initdb.d/restorepoint.sql || true
echo ">>> Base restore complete"
