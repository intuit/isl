#!/usr/bin/env bash
# ISL Command Line Runner for Linux/Mac
#
# This script allows you to run ISL commands without manually invoking Gradle
# Usage: ./isl.sh [command] [options]
# Examples:
#   ./isl.sh --version
#   ./isl.sh info
#   ./isl.sh transform script.isl -i input.json

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

# Check if the shadow JAR exists
JAR_FILE="$SCRIPT_DIR/isl-cmd/build/libs/isl-2.4.20-SNAPSHOT.jar"

if [ -f "$JAR_FILE" ]; then
    # Use the pre-built JAR
    java -jar "$JAR_FILE" "$@"
else
    # Fall back to Gradle
    echo "Shadow JAR not found. Building and running via Gradle..."
    echo "Run './gradlew :isl-cmd:shadowJar' to build the JAR for faster startup."
    echo ""
    cd "$SCRIPT_DIR"
    ./gradlew :isl-cmd:run --quiet --console=plain --args="$*"
fi

