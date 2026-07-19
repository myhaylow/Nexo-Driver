#!/usr/bin/env bash
set -euo pipefail

npm install --ignore-scripts
npm test
npm run validate:fixtures

if command -v gradle >/dev/null; then
  gradle -p android tasks --no-daemon >/dev/null
fi
