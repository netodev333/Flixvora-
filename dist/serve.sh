#!/bin/sh
# Idempotent starter for the two static file servers that serve dist/.
# Safe to call any number of times: it only starts a port that is not already up.
DIST=/workspace/project/dist

port_up() {
  python3 - "$1" <<'PY' >/dev/null 2>&1
import socket, sys
s = socket.socket()
s.settimeout(1)
sys.exit(0 if s.connect_ex(("127.0.0.1", int(sys.argv[1]))) == 0 else 1)
PY
}

for p in 12000 12001; do
  if ! port_up "$p"; then
    setsid python3 -m http.server "$p" --bind 0.0.0.0 --directory "$DIST" \
      >/dev/null 2>&1 </dev/null &
  fi
done
