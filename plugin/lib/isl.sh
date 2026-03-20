#!/usr/bin/env bash
# ISL Command Line Runner for Linux/Mac (plugin lib - runs embedded JAR)
#
# Usage: ./isl.sh [command] [options]
# Examples:
#   ./isl.sh --version
#   ./isl.sh info
#   ./isl.sh transform script.isl -i input.json
#   ./isl.sh test [path]

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
JAR_FILE="$SCRIPT_DIR/isl-cmd-all.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: isl-cmd-all.jar not found in $SCRIPT_DIR"
    exit 1
fi

exec java -jar "$JAR_FILE" "$@"
