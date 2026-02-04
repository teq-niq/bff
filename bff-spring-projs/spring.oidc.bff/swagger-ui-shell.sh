#!/bin/bash

# Absolute path to project
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Force swagger-ui as working directory
cd "$PROJECT_DIR/swagger-ui" || exit

# Isolated PATH (does NOT include existing PATH)
export PATH="$PROJECT_DIR/../node:$PROJECT_DIR/swagger-ui/node_modules/.bin:$GIT_HOME:$VS_CODE_HOME/bin"

echo "Using isolated Node environment:"
node -v
npm -v

# Start  cmd shell in swagger-ui

exec "$SHELL"
