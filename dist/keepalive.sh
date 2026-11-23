#!/bin/sh
# Serves dist/ on :12000 and restarts it if the process ever dies.
# Started with setsid so it survives the parent shell exiting.
while true; do
  python3 -m http.server 12000 --bind 0.0.0.0 --directory /workspace/project/dist
  sleep 2
done
