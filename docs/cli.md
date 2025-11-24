---
title: Command Line Interface
nav_order: 4
description: "ISL CLI is a powerful command-line tool for running ISL transformation scripts. Supports JSON/YAML input/output, variables, parameters, and multiple formats."
excerpt: "ISL CLI is a powerful command-line tool for running ISL transformation scripts. Supports JSON/YAML input/output, variables, parameters, and multiple formats."
---

The ISL CLI is a powerful command-line tool for running ISL transformation scripts. It supports JSON and YAML input/output, variables, parameters, and multiple output formats.

## Installation

### Prerequisites

- **Java 21 or higher** - ISL requires Java 21+
- **Gradle 8.5+** (for building from source)

You can check your Java version:

```bash
java -version
```

### Option 1: Quick Start (Recommended)

If you've cloned the ISL repository, you can use the pre-built wrapper scripts:

**Linux/Mac:**
```bash
# Make the script executable
chmod +x isl.sh

# Run ISL commands
./isl.sh --version
./isl.sh transform script.isl -i input.json
```

**Windows:**
```cmd
# Run ISL commands
isl.bat --version
isl.bat transform script.isl -i input.json
```

These wrapper scripts will automatically build the JAR if needed and run it.

### Option 2: Build the JAR

Build the standalone executable JAR:

```bash
# Build the fat JAR
./gradlew :isl-cmd:shadowJar

# The JAR will be created at:
# isl-cmd/build/libs/isl-[version]-SNAPSHOT.jar
```

### Option 3: System-Wide Installation

After building the JAR, install it system-wide:

**Linux/Mac:**

```bash
# Option A: Create an alias (add to ~/.bashrc or ~/.zshrc)
alias isl='java -jar /path/to/isl-[version]-SNAPSHOT.jar'

# Option B: Create a wrapper script
sudo tee /usr/local/bin/isl > /dev/null << 'EOF'
#!/bin/bash
java -jar /path/to/isl-[version]-SNAPSHOT.jar "$@"
EOF
sudo chmod +x /usr/local/bin/isl

# Then use it anywhere:
isl --version
```

**Windows:**

```cmd
# Create isl.bat in a directory that's in your PATH (e.g., C:\Windows\System32)
@echo off
java -jar C:\path\to\isl-[version]-SNAPSHOT.jar %*
```

### Option 4: Run with Gradle (Development)

For development, you can run directly with Gradle:

```bash
./gradlew :isl-cmd:run --args="transform script.isl -i input.json"

# Or use the runIsl task
./gradlew :isl-cmd:runIsl --args="transform script.isl -i input.json"
```

## Basic Usage

The ISL CLI has three main commands:

### 1. Transform Command

Execute an ISL transformation script:

```bash
isl transform <script.isl> [options]
```

**Basic Examples:**

```bash
# Transform with input from stdin
echo '{"name":"Alice"}' | isl transform script.isl

# Transform with input file
isl transform script.isl -i input.json

# Save output to file
isl transform script.isl -i input.json -o output.json

# Pretty-print output
isl transform script.isl -i input.json --pretty

# Output as YAML
isl transform script.isl -i input.json -f yaml
```

### 2. Validate Command

Check if an ISL script is syntactically valid:

```bash
# Validate a script
isl validate script.isl

# If valid, returns nothing and exit code 0
# If invalid, shows error details and exit code 1
```

### 3. Info Command

Display version and system information:

```bash
isl info
```

## Command Options

### Transform Command Options

| Option | Alias | Description |
|--------|-------|-------------|
| `--input FILE` | `-i` | Input data file (JSON or YAML) |
| `--output FILE` | `-o` | Output file (default: stdout) |
| `--vars FILE` | `-v` | Variables file (JSON or YAML) |
| `--param KEY=VALUE` | `-p` | Command-line parameter (can be repeated) |
| `--format FORMAT` | `-f` | Output format: `json`, `yaml`, `pretty-json` |
| `--pretty` | | Pretty-print JSON output (shorthand for `-f pretty-json`) |
| `--function NAME` | | Function to execute (default: `run`) |

### Global Options

| Option | Alias | Description |
|--------|-------|-------------|
| `--help` | `-h` | Show help message |
| `--version` | `-V` | Show version information |

### Environment Variables

- `debug=true` - Enable debug output and stack traces

```bash
# Enable debug mode
debug=true isl transform script.isl -i input.json
```

## Working with Input Data

### Using Input Files

The input file data is accessible as the `$input` variable in your ISL script:

**input.json:**
```json
{
  "name": "Alice",
  "age": 30
}
```

**script.isl:**
```isl
fun run( $input ) {
    greeting: `Hello, ${ $input.name }!`,
    message: `You are ${ $input.age } years old.`
}
```

**Run:**
```bash
isl transform script.isl -i input.json --pretty
```

**Output:**
```json
{
  "greeting": "Hello, Alice!",
  "message": "You are 30 years old."
}
```

### Using Command-Line Parameters

Pass variables directly from the command line using `--param` (or `-p`):

**script.isl:**
```isl
fun run($name, $age, $isPremium) {
    greeting: `Hello, $name!`,
    age: $age,
    status: $isPremium ? "Premium user" : "Standard user"
}
```

**Usage:**
```bash
# Basic parameters
isl transform script.isl -p name=Alice -p age=30 -p isPremium=true --pretty
```

#### Parameter Types

The CLI automatically detects parameter types:

```bash
# Strings (default)
-p name=Alice
-p message="Hello World"

# Numbers (integers and decimals)
-p age=30
-p price=19.99
-p discount=0.15

# Booleans (lowercase)
-p active=true
-p debug=false

# Null
-p optionalField=null

# JSON objects (use quotes)
-p 'config={"env":"prod","port":8080}'

# JSON arrays
-p 'ids=[1,2,3,4,5]'
-p 'tags=["admin","premium","verified"]'

# Nested structures
-p 'user={"name":"Alice","profile":{"age":30,"city":"NYC"}}'
```

#### Platform-Specific Quoting

**Linux/Mac:** Use single quotes for JSON values:
```bash
./isl.sh transform script.isl -p 'config={"env":"prod","port":8080}'
```

**Windows:** Use double quotes with escaped inner quotes:
```cmd
isl.bat transform script.isl -p "config={\"env\":\"prod\",\"port\":8080}"
```

### Using Variables Files

Store reusable configuration in YAML or JSON files:

**vars.yaml:**
```yaml
apiKey: sk_live_abc123
environment: production
timeout: 30
features:
  logging: true
  metrics: true
```

**script.isl:**
```isl
fun run($apiKey, $environment, $timeout, $features) {
    config: {
        apiKey: $apiKey,
        env: $environment,
        timeout: $timeout,
        features: $features
    }
}
```

**Usage:**
```bash
isl transform script.isl -v vars.yaml --pretty
```

### Combining Input Sources

You can combine input files, variables files, and command-line parameters:

```bash
isl transform script.isl \
  -i data.json \
  -v vars.yaml \
  -p environment=staging \
  -p debug=true
```

**Parameter Precedence (highest to lowest):**
1. Command-line `--param` (overrides everything)
2. Variables from `--vars` file
3. Input data from `--input` (accessible as `$input`)

## Examples

### Example 1: Simple Hello World

**hello.isl:**
```isl
fun run($input) {
    result: {
        message: `Hello, ${ $input.name }!`,
        timestamp: @.Date.Now()
    }
}
```

**input.json:**
```json
{
  "name": "World"
}
```

**Run:**
```bash
isl transform hello.isl -i input.json --pretty
```

**Output:**
```json
{
  "result": {
    "message": "Hello, World!",
    "timestamp": "2025-11-20T10:30:00.000Z"
  }
}
```

### Example 2: Data Transformation with Filtering

**process.isl:**
{% raw %}
```isl
fun run($data) {
    // Filter and transform active items
    activeItems: foreach $item in $data.items | filter($it.active == true)
        {
            id: $item.id,
            name: $item.name | upperCase,
            total: {{ $item.price * $item.quantity }}
        }
    endfor,
    
    // Calculate grand total
    grandTotal: $data.items 
        | filter($it.active == true)
        | map({{ $it.price * $it.quantity }})
        | reduce({{ $acc + $it }})
}
```
{% endraw %}

**data.json:**
```json
{
  "items": [
    {"id": 1, "name": "apple", "price": 1.5, "quantity": 3, "active": true},
    {"id": 2, "name": "banana", "price": 0.8, "quantity": 5, "active": false},
    {"id": 3, "name": "orange", "price": 2.0, "quantity": 2, "active": true}
  ]
}
```

**Run:**
```bash
isl transform process.isl -i data.json --pretty
```

### Example 3: API Configuration with Parameters

**api-call.isl:**
```isl
fun run($endpoint, $apiKey, $timeout, $retries) {
    request: {
        url: $endpoint,
        headers: {
            Authorization: `Bearer $apiKey`
        },
        timeout: $timeout,
        retryCount: $retries
    }
}
```

**Usage:**
```bash
isl transform api-call.isl \
  -p endpoint=https://api.example.com/users \
  -p apiKey=sk_live_abc123 \
  -p timeout=30 \
  -p retries=3 \
  --pretty
```

### Example 4: Deployment Configuration

**deploy.isl:**
```isl
fun run($environment, $version, $replicas, $features, $resources) {
    deployment: {
        environment: $environment,
        version: $version,
        replicas: $replicas,
        features: $features,
        resources: $resources,
        deployedAt: @.Date.Now() | to.string("yyyy-MM-dd HH:mm:ss")
    }
}
```

**Linux/Mac:**
```bash
./isl.sh transform deploy.isl \
  -p environment=production \
  -p version=2.1.5 \
  -p replicas=3 \
  -p 'features={"monitoring":true,"logging":true,"metrics":true}' \
  -p 'resources={"cpu":"2000m","memory":"4Gi"}' \
  --pretty
```

**Windows:**
```cmd
isl.bat transform deploy.isl ^
  -p environment=production ^
  -p version=2.1.5 ^
  -p replicas=3 ^
  -p "features={\"monitoring\":true,\"logging\":true,\"metrics\":true}" ^
  -p "resources={\"cpu\":\"2000m\",\"memory\":\"4Gi\"}" ^
  --pretty
```

### Example 5: Data Enrichment Pipeline

**enrich.isl:**
```isl
fun run($input, $region, $tags, $metadata) {
    enriched: {
        originalData: $input,
        region: $region,
        tags: $tags,
        metadata: $metadata,
        processedAt: @.Date.Now()
    }
}
```

**Usage:**
```bash
# Combine input file with additional parameters
isl transform enrich.isl \
  -i customer-data.json \
  -p region=us-east-1 \
  -p 'tags=["priority","verified"]' \
  -p 'metadata={"source":"api","version":"v2"}' \
  -o enriched-data.json
```

### Example 6: Multi-Stage Pipeline

Process data through multiple transformation stages:

```bash
# Stage 1: Extract and normalize
isl transform extract.isl -i raw-data.json -o stage1.json

# Stage 2: Enrich with metadata
isl transform enrich.isl -i stage1.json -p source=api -o stage2.json

# Stage 3: Format for output
isl transform format.isl -i stage2.json -o final-output.json --pretty
```

### Example 7: Calling Specific Functions

By default, ISL calls the `run` function. You can call other functions:

**utils.isl:**
{% raw %}
```isl
fun processOrders($input) {
    orders: foreach $order in $input.orders
        {
            id: $order.id,
            total: {{ $order.amount * 1.1 }}
        }
    endfor
}

fun processCustomers($input) {
    customers: foreach $customer in $input.customers
        {
            name: $customer.name | upperCase,
            email: $customer.email
        }
    endfor
}
```
{% endraw %}

**Usage:**
```bash
# Call the processOrders function
isl transform utils.isl --function processOrders -i data.json

# Call the processCustomers function
isl transform utils.isl --function processCustomers -i data.json
```

## Output Formats

### JSON (Default)

```bash
isl transform script.isl -i input.json
# Compact JSON output
```

### Pretty JSON

```bash
# Option 1: Use --pretty flag
isl transform script.isl -i input.json --pretty

# Option 2: Use --format
isl transform script.isl -i input.json -f pretty-json
```

### YAML

```bash
isl transform script.isl -i input.json -f yaml
```

## Tips and Best Practices

### 1. Validate Before Running

Always validate your scripts to catch syntax errors early:

```bash
isl validate script.isl
```

### 2. Use Pretty Print for Debugging

When debugging, use `--pretty` for readable output:

```bash
isl transform script.isl -i input.json --pretty
```

### 3. Use Variables Files for Configuration

Store environment-specific configuration in variables files:

```bash
# Development
isl transform script.isl -v vars.dev.yaml

# Production
isl transform script.isl -v vars.prod.yaml
```

### 4. Override with Command-Line Parameters

Use variables files for defaults and override with CLI params:

```bash
isl transform script.isl \
  -v vars.yaml \
  -p environment=staging \
  -p debug=true
```

### 5. Build Transformation Pipelines

Chain multiple transformations together:

```bash
# Unix pipe
cat data.json | isl transform step1.isl | isl transform step2.isl

# Or with files
isl transform step1.isl -i data.json -o temp.json
isl transform step2.isl -i temp.json -o final.json
```

### 6. Use Line Continuation for Long Commands

**Linux/Mac:**
```bash
isl transform script.isl \
  -i input.json \
  -v vars.yaml \
  -p environment=production \
  -p debug=false \
  --pretty
```

**Windows:**
```cmd
isl.bat transform script.isl ^
  -i input.json ^
  -v vars.yaml ^
  -p environment=production ^
  -p debug=false ^
  --pretty
```

### 7. Create Aliases for Common Tasks

Add to your `~/.bashrc` or `~/.zshrc`:

```bash
# Quick transform with pretty output
alias islt='isl transform --pretty'

# Validate and transform
alias islvt='isl validate $1 && isl transform $1'
```

## Troubleshooting

### Script Not Found

**Error:** `Could not find script.isl`

**Solution:** Verify the path to your ISL script:

```bash
# Use absolute path
isl transform /full/path/to/script.isl

# Or relative path from current directory
isl transform ./scripts/my-script.isl

# Check if file exists
ls -la script.isl
```

### Input File Not Found

**Error:** `Could not read input file`

**Solution:** Verify the input file exists and is readable:

```bash
# Check file exists
ls -la input.json

# Use explicit path
isl transform script.isl -i ./input.json
```

### Syntax Errors

**Error:** Various parsing errors

**Solution:** Validate your script:

```bash
isl validate script.isl
```

Review the error message and fix the syntax according to the [ISL Guide for AI](https://intuit.github.io/isl/ai/).

### JSON Parameter Parsing Errors

**Error:** `Failed to parse parameter value`

**Solution:** Check your JSON quoting:

**Linux/Mac:**
```bash
# Use single quotes around JSON
-p 'config={"key":"value"}'
```

**Windows:**
```cmd
# Use double quotes with escaped inner quotes
-p "config={\"key\":\"value\"}"
```

### Parameter Not Found in Script

**Error:** `Variable $paramName not found`

**Solution:** 
- Ensure you're passing the parameter: `-p paramName=value`
- Check the parameter name matches the function signature
- Remember variables in ISL use the `$` prefix

### JAR Not Found (When Using Wrapper Scripts)

**Error:** `Shadow JAR not found`

**Solution:** Build the JAR:

```bash
./gradlew :isl-cmd:shadowJar
```

The wrapper scripts will automatically fall back to Gradle if the JAR isn't built, but building it provides faster startup times.

### Java Version Issues

**Error:** `Unsupported class file major version` or similar

**Solution:** Ensure you have Java 21 or higher:

```bash
java -version
# Should show Java 21 or higher
```

If you have multiple Java versions installed, ensure Java 21+ is in your PATH or use `JAVA_HOME`:

```bash
export JAVA_HOME=/path/to/java21
export PATH=$JAVA_HOME/bin:$PATH
```

### Debug Mode

Enable debug mode for detailed error information:

```bash
debug=true isl transform script.isl -i input.json
```

This will show:
- Stack traces for errors
- Detailed parsing information
- Runtime execution details

## Advanced Usage

### Working with Stdin/Stdout

```bash
# Read from stdin, write to stdout
echo '{"name":"Alice"}' | isl transform script.isl

# Chain transformations
cat data.json | isl transform step1.isl | isl transform step2.isl > output.json

# Use in shell scripts
result=$(isl transform script.isl -i input.json)
echo "Result: $result"
```

### Integration with Other Tools

```bash
# Use with curl for API processing
curl -s https://api.example.com/data | isl transform process.isl --pretty

# Use with jq for pre-processing
cat data.json | jq '.items' | isl transform process.isl

# Use in CI/CD pipelines
if isl validate deployment.isl; then
  isl transform deployment.isl -i config.json -o deploy.json
  kubectl apply -f deploy.json
fi
```

### Batch Processing

**Linux/Mac:**
```bash
# Process multiple files
for file in data/*.json; do
  isl transform process.isl -i "$file" -o "output/$(basename "$file")"
done
```

**Windows:**
```cmd
REM Process multiple files
for %%f in (data\*.json) do (
  isl.bat transform process.isl -i "%%f" -o "output\%%~nxf"
)
```

## Next Steps

- **Learn ISL Syntax**: Check out the [ISL Guide for AI](https://intuit.github.io/isl/ai/) for complete syntax reference
- **Quick Start**: Follow the [Quick Start Guide](https://intuit.github.io/isl/quickstart) for ISL basics
- **Explore Examples**: See more examples in the [Examples section](https://intuit.github.io/isl/examples/dates)
- **Integration**: Learn about [Java/Kotlin Integration](https://intuit.github.io/isl/java.start) for embedding ISL in applications

## Getting Help

- **GitHub Issues**: [Report bugs or request features](https://github.com/intuit/isl/issues)
- **Documentation**: [Full ISL documentation](https://intuit.github.io/isl/)
- **Examples**: Check the `isl-cmd/examples/` directory in the repository

## License

ISL is licensed under the Apache License 2.0.

