#!/bin/bash

echo "🎨 Starting ISL Playground Frontend..."
echo "Frontend will be available at: http://localhost:3000"
echo ""

cd "$(dirname "$0")/frontend"

if [ ! -d "node_modules" ]; then
  echo "📦 Installing dependencies..."
  npm install
fi

npm run dev

