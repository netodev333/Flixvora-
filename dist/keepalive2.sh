#!/bin/sh
# Second port so the app has a fallback host if one is blocked on a carrier.
while true; do
  python3 -m http.server 12001 --bind 0.0.0.0 --directory /workspace/project/dist
  sleep 2
done
