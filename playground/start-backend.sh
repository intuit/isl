#!/bin/bash

echo "🚀 Starting ISL Playground Backend..."
echo "Backend will be available at: http://localhost:8080"
echo ""

cd "$(dirname "$0")/backend"
../../gradlew bootRun

