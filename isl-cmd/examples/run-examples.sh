#!/bin/bash

# Example usage scripts for ISL CLI

echo "=== ISL CLI Examples ==="
echo ""

# Ensure we're in the right directory
cd "$(dirname "$0")"

# Read version from gradle.properties
VERSION=$(grep "^version=" "../../gradle.properties" | cut -d'=' -f2 | tr -d '\r')

# Shadow JAR has classifier -all
JAR="../build/libs/isl-$VERSION-all.jar"
if [ ! -f "$JAR" ]; then
  JAR="../build/libs/isl-$VERSION.jar"
fi
if [ ! -f "$JAR" ]; then
    echo "Building ISL CLI..."
    cd ..
    ./gradlew shadowJar
    cd examples
    JAR="../build/libs/isl-$VERSION-all.jar"
    [ ! -f "$JAR" ] && JAR="../build/libs/isl-$VERSION.jar"
fi

ISL="java -jar $JAR"

echo "1. Simple Hello World"
echo "   Command: isl transform hello.isl -i hello-input.json --pretty"
$ISL transform hello.isl -i hello-input.json --pretty
echo ""
echo ""

echo "2. Data Transformation"
echo "   Command: isl transform transform.isl -i data.json --pretty"
$ISL transform transform.isl -i data.json --pretty
echo ""
echo ""

echo "3. With Command-Line Parameters"
echo "   Command: isl transform hello.isl -p name=Alice --pretty"
$ISL transform hello.isl -p name=Alice --pretty
echo ""
echo ""

echo "4. Validate Script"
echo "   Command: isl validate transform.isl"
$ISL validate transform.isl
echo ""
echo ""

echo "5. Show Info"
echo "   Command: isl info"
$ISL info
echo ""
echo ""

echo "6. Run Tests (ISL + YAML suites)"
echo "   Command: isl test ."
$ISL test .
echo ""


