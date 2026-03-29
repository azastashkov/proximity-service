#!/bin/bash
set -e

echo "Waiting for primary to be ready..."
until PGPASSWORD=replicator_pass pg_isready -h postgres-primary -p 5432 -U replicator 2>/dev/null; do
    sleep 2
done

if [ ! -f "$PGDATA/PG_VERSION" ]; then
    echo "Running pg_basebackup from primary..."
    rm -rf "$PGDATA"/*
    PGPASSWORD=replicator_pass pg_basebackup \
        -h postgres-primary -D "$PGDATA" -U replicator -Fp -Xs -P -R
    chown -R postgres:postgres "$PGDATA"
    chmod 700 "$PGDATA"
    echo "Base backup complete. Starting replica..."
fi

exec gosu postgres postgres
