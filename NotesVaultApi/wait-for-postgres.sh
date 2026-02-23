#!/bin/bash
set -e
host="$1"
shift
until pg_isready -h "$host" -p 5432
do
  echo "Waiting for Postgres at $host:5432..."
  sleep 2
done
echo "Postgres is ready!"
exec "$@"