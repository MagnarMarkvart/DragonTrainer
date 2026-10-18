#!/usr/bin/env bash
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
back="$root/dragontrainer-bigbank"
front="$root/DragonTrainer"

if [[ ! -f "$back/.env" ]]; then
  cp "$back/.env.example" "$back/.env"
  echo "Created dragontrainer-bigbank/.env from .env.example"
fi

port_open() {
  if command -v nc >/dev/null 2>&1; then
    nc -z 127.0.0.1 "$1"
    return
  fi
  (echo >/dev/tcp/127.0.0.1/"$1") >/dev/null 2>&1
}

wait_port() {
  local port="$1"
  local name="$2"
  local i
  for i in $(seq 1 150); do
    if port_open "$port"; then
      return 0
    fi
    sleep 2
  done
  echo "$name did not open port $port" >&2
  exit 1
}

cd "$back"
state="$(docker inspect -f '{{.State.Running}}' dragontrainer-postgres 2>/dev/null || true)"
if [[ "$state" != "true" ]]; then
  echo "Starting Postgres..."
  docker compose up -d
else
  echo "Postgres is already running."
fi

wait_port 5432 "Postgres"

if port_open 8080; then
  echo "Backend is already listening on port 8080."
else
  echo "Starting backend..."
  if [[ "${OSTYPE:-}" == msys* || "${OSTYPE:-}" == cygwin* ]]; then
    nohup cmd.exe //c "gradlew.bat bootRun" >/dev/null 2>&1 &
  else
    nohup ./gradlew bootRun >/dev/null 2>&1 &
  fi
  wait_port 8080 "Backend"
fi

cd "$front"
front_port=""
for port in $(seq 4200 4210); do
  if port_open "$port"; then
    front_port="$port"
    echo "Frontend is already listening on port $front_port."
    break
  fi
done

if [[ -z "$front_port" ]]; then
  front_port=4200
  echo "Starting frontend on port $front_port..."
  nohup npm start -- --port "$front_port" >/dev/null 2>&1 &
  deadline=$((SECONDS + 300))
  while (( SECONDS < deadline )); do
    if port_open "$front_port"; then
      break
    fi
    for port in $(seq 4200 4210); do
      if port_open "$port"; then
        front_port="$port"
        echo "Frontend opened on port $front_port."
        break 2
      fi
    done
    sleep 2
  done
  if ! port_open "$front_port"; then
    echo "Frontend did not open a port between 4200 and 4210" >&2
    exit 1
  fi
fi

echo
echo "Front: http://localhost:$front_port"
echo "Back:  http://localhost:8080"
