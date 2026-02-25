# ISL Runtime Library

This directory contains the embedded ISL command-line runtime that allows the extension to execute ISL transformations and run tests without requiring users to build the project.

## Contents

- `isl-cmd-all.jar` - ISL command-line tool with all dependencies (shadow JAR, ~35MB)

## How It Works

When you click the "▶ Run" button above an ISL function, the extension:

1. Prompts for input JSON
2. Saves the input to a temporary file
3. Executes: `java -jar isl-cmd-all.jar transform <file>.isl -i input.json -f <function>`
4. Shows the output in the terminal

The ISL CLI also supports running tests: `java -jar isl-cmd-all.jar test [path]`

## Requirements

- **Java 11 or later** must be installed and available in PATH
- Or configure `isl.execution.javaHome` in VS Code settings

## Building

Build the fat JAR and copy it here using:

```bash
./gradlew copyIslToPlugin
```

This builds the shadow JAR from `isl-cmd` and copies it to `plugin/lib/isl-cmd-all.jar`.

To build only the JAR (without copying):

```bash
./gradlew :isl-cmd:shadowJar
```

The output is `isl-cmd/build/libs/isl-<version>-all.jar`.

## Version

Built from ISL version: **1.1.0**

## Distribution

This JAR is included in the extension package (`.vsix`) so end users don't need to:
- Clone the ISL repository
- Build the project with Gradle
- Configure complex paths

Everything works out of the box with just Java installed!

## Size Note

The JAR is approximately 35MB because it includes all dependencies:
- ISL transform engine
- ANTLR parser
- Kotlin runtime
- Jackson JSON library
- All other required libraries

This makes the extension self-contained and easy to distribute.

