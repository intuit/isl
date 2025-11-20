# ISL Command Line Interface

Command-line tool for running ISL transformation scripts.

## Building

```bash
# Build the fat JAR
./gradlew :isl-cmd:shadowJar

# The executable JAR will be in: isl-cmd/build/libs/isl-[version]-SNAPSHOT.jar
```

## Installation

**Note:** Bash files already exists in the root folder.

```bash
# Option 1: Create an alias
alias isl='java -jar /path/to/isl-[version]-SNAPSHOT.jar'

# Option 2: Create a wrapper script (Unix/Mac)
cat > /usr/local/bin/isl << 'EOF'
#!/bin/bash
java -jar /path/to/isl-[version]-SNAPSHOT.jar "$@"
EOF
chmod +x /usr/local/bin/isl

# Option 3: Create a batch file (Windows)
# Create isl.bat with:
@echo off
java -jar C:\path\to\isl-[version]-SNAPSHOT.jar %*
```

## Usage

### Transform Command

Execute an ISL transformation script:

```bash
# Basic usage
isl transform script.isl

# With input file
isl transform script.isl -i input.json

# With output file
isl transform script.isl -i input.json -o output.json

# With variables file
isl transform script.isl -i input.json -v vars.yaml

# With command-line parameters
isl transform script.isl -p name=John -p age=30

# With pretty output
isl transform script.isl -i input.json --pretty

# Output as YAML
isl transform script.isl -i input.json -f yaml

# Call specific function
isl transform script.isl --function processData -i input.json
```

### Validate Command

Check if a script is syntactically valid:

```bash
isl validate script.isl
```

### Info Command

Show version and system information:

```bash
isl info
```

## Examples

### Example 1: Simple Transformation

**script.isl**:
```isl
fun run($input) {
    result: {
        message: `Hello, ${ $input.name }!`,
        timestamp: @.Date.Now()
    }
}
```

**input.json**:
```json
{
  "name": "World"
}
```

**Run**:
```bash
isl transform script.isl -i input.json --pretty
```

**Output**:
```json
{
  "result": {
    "message": "Hello, World!",
    "timestamp": "2024-11-16T10:30:00.000Z"
  }
}
```

### Example 2: Data Processing

**process.isl**:
```isl
fun run($data) {
    items: foreach $item in $data.items | filter($item.active) {
        id: $item.id,
        name: $item.name | upperCase,
        total: {{ $item.price * $item.quantity }}
    }
    endfor,
    
    total: $data.items | reduce({{ $acc + $it.price * $it.quantity }})
}
```

**data.json**:
```json
{
  "items": [
    {"id": 1, "name": "apple", "price": 1.5, "quantity": 3, "active": true},
    {"id": 2, "name": "banana", "price": 0.8, "quantity": 5, "active": false},
    {"id": 3, "name": "orange", "price": 2.0, "quantity": 2, "active": true}
  ]
}
```

**Run**:
```bash
isl transform process.isl -i data.json --pretty
```

### Example 3: Using Command-Line Parameters

The `--param` (or `-p`) option lets you pass variables directly from the command line without needing a separate file.

**template.isl**:
```isl
fun run($name, $age, $isPremium, $discount) {
    output: {
        greeting: "Hello, $name!",
        age: $age,
        premiumStatus: $isPremium,
        discountRate: $discount,
        message: $isPremium ? "Premium user" : "Standard user"
    }
}
```

**Basic usage with strings and numbers**:
```bash
isl transform template.isl -p name=Alice -p age=30
```

**Different data types**:
```bash
# String values (default)
isl transform template.isl -p name=John

# Numbers (integers and decimals)
isl transform template.isl -p age=25 -p discount=0.15

# Booleans
isl transform template.isl -p isPremium=true -p isActive=false

# Null values
isl transform template.isl -p optionalField=null
```

**Complex JSON objects and arrays**:
```bash
# JSON object
isl transform template.isl -p 'config={"env":"prod","timeout":30}'

# JSON array
isl transform template.isl -p 'items=[1,2,3,4,5]'

# Nested JSON
isl transform template.isl -p 'user={"name":"Alice","tags":["admin","premium"]}'
```

**Real-world example - Configuration with multiple params**:

**deploy.isl**:
```isl
fun run($environment, $version, $replicas, $features) {
    deployment: {
        environment: $environment,
        version: $version,
        replicas: $replicas,
        features: $features,
        timestamp: @.Date.Now()
    }
}
```

```bash
isl transform deploy.isl \
  -p environment=production \
  -p version=2.1.5 \
  -p replicas=3 \
  -p 'features={"monitoring":true,"logging":true,"metrics":true}' \
  --pretty
```

**Combining parameters with input files**:

**enrich.isl**:
```isl
fun run($input, $apiKey, $region, $options) {
    enriched: {
        data: $input,
        metadata: {
            apiKey: $apiKey,
            region: $region,
            options: $options,
            processedAt: @.Date.Now()
        }
    }
}
```

```bash
# Input from file + command-line params
isl transform enrich.isl \
  -i data.json \
  -p apiKey=sk_live_123456 \
  -p region=us-east-1 \
  -p 'options={"cache":true,"ttl":3600}'
```

**Parameter precedence**:
Parameters from different sources are merged with this precedence (highest to lowest):
1. Command-line `--param` (highest priority)
2. Variables from `--vars` file
3. Input data from `--input` (available as `$input`)

```bash
# vars.yaml contains: { "name": "Bob", "age": 25 }
# Command-line params override vars file
isl transform script.isl -v vars.yaml -p name=Alice
# Result: name="Alice", age=25
```

**Tips for using --param**:
- Use single quotes `'` for JSON values to avoid shell interpretation
- Boolean values: `true` or `false` (lowercase)
- Numbers: integers (`42`) or decimals (`3.14`)
- Strings: No quotes needed unless they contain special characters
- Multiple values: Repeat `-p` for each parameter
- All params are accessible in your ISL script as variables (with `$` prefix)

### Example 4: Using Variables Files

**vars.yaml**:
```yaml
greeting: "Welcome"
env: "production"
```

**Run**:
```bash
isl transform template.isl -p name=Alice -v vars.yaml
```

### Example 5: Pipeline Processing

```bash
# Transform, then filter, then aggregate
isl transform step1.isl -i raw-data.json -o temp1.json
isl transform step2.isl -i temp1.json -o temp2.json
isl transform step3.isl -i temp2.json -o final-result.json
```

## Command Reference

### Global Options

- `--help`, `-h` - Show help message
- `--version`, `-V` - Show version information

### Transform Command Options

| Option | Alias | Description |
|--------|-------|-------------|
| `--input FILE` | `-i` | Input data file (JSON or YAML) |
| `--output FILE` | `-o` | Output file (default: stdout) |
| `--vars FILE` | `-v` | Variables file (JSON or YAML) |
| `--param KEY=VALUE` | `-p` | Command-line parameter (can be repeated) |
| `--format FORMAT` | `-f` | Output format: json, yaml, pretty-json |
| `--pretty` | | Pretty print JSON output |
| `--function NAME` | | Function to execute (default: run) |

### Environment Variables

- `debug=true` - Enable debug output and stack traces

## Development

### Running in Development

```bash
# Run directly with Gradle
./gradlew :isl-cmd:run --args="transform script.isl -i input.json"

# Or use the runIsl task
./gradlew :isl-cmd:runIsl --args="transform script.isl -i input.json"
```

### Testing

```bash
# Run tests
./gradlew :isl-cmd:test

# Run with coverage
./gradlew :isl-cmd:jacocoTestReport
```

## Troubleshooting

### Script Not Found

Make sure the path to your ISL script is correct:
```bash
# Use absolute path
isl transform /full/path/to/script.isl

# Or relative path from current directory
isl transform ./scripts/my-script.isl
```

### Input File Not Found

Verify the input file exists:
```bash
ls -la input.json
isl transform script.isl -i ./input.json
```

### Syntax Errors

Validate your script first:
```bash
isl validate script.isl
```

### Debug Mode

Enable debug output for detailed error information:
```bash
debug=true isl transform script.isl -i input.json
```

## Tips

1. **Use `--pretty` for readable output** when debugging
2. **Validate scripts** before running them on important data
3. **Use variables files** for configuration that changes between environments
4. **Pipeline commands** with shell pipes for complex workflows
5. **Set up aliases** for frequently used commands

## License

Apache License 2.0


