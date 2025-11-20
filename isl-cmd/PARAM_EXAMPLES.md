# ISL Command Line - Parameter Examples

This file demonstrates all the ways you can use the `--param` (or `-p`) option with ISL CMD.

## Basic Data Types

### Strings
```bash
# Simple string (no quotes needed)
isl transform script.isl -p name=Alice
isl transform script.isl -p message="Hello World"

# String with spaces (use quotes)
isl transform script.isl -p message="Hello from ISL"
```

### Numbers
```bash
# Integers
isl transform script.isl -p age=30 -p count=100

# Decimals/Floats
isl transform script.isl -p price=19.99 -p tax=0.08
```

### Booleans
```bash
# Boolean values (lowercase)
isl transform script.isl -p active=true -p debug=false
```

### Null
```bash
# Null value
isl transform script.isl -p optionalField=null
```

## Complex Data Types

### JSON Objects

**Windows (use double quotes and escape inner quotes):**
```cmd
isl.bat transform script.isl -p "config={\"env\":\"prod\",\"port\":8080}"
```

**Linux/Mac (use single quotes):**
```bash
./isl.sh transform script.isl -p 'config={"env":"prod","port":8080}'
```

### JSON Arrays

**Windows:**
```cmd
isl.bat transform script.isl -p "ids=[1,2,3,4,5]"
isl.bat transform script.isl -p "tags=[\"admin\",\"premium\",\"verified\"]"
```

**Linux/Mac:**
```bash
./isl.sh transform script.isl -p 'ids=[1,2,3,4,5]'
./isl.sh transform script.isl -p 'tags=["admin","premium","verified"]'
```

### Nested Structures

**Windows:**
```cmd
isl.bat transform script.isl -p "user={\"name\":\"Alice\",\"profile\":{\"age\":30,\"city\":\"NYC\"}}"
```

**Linux/Mac:**
```bash
./isl.sh transform script.isl -p 'user={"name":"Alice","profile":{"age":30,"city":"NYC"}}'
```

## Practical Examples

### Example 1: API Configuration

**api-call.isl:**
```isl
fun run($endpoint, $apiKey, $timeout, $retries) {
    request: {
        url: $endpoint,
        headers: {
            "Authorization": "Bearer $apiKey"
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

### Example 2: Deployment Configuration

**deploy.isl:**
```isl
fun run($environment, $version, $replicas, $features, $resources) {
    deployment: {
        environment: $environment,
        version: $version,
        replicas: $replicas,
        features: $features,
        resources: $resources,
        deployedAt: @.Date.Now()
    }
}
```

**Usage (Linux/Mac):**
```bash
./isl.sh transform deploy.isl \
  -p environment=production \
  -p version=2.1.5 \
  -p replicas=3 \
  -p 'features={"monitoring":true,"logging":true,"metrics":true}' \
  -p 'resources={"cpu":"2000m","memory":"4Gi"}' \
  --pretty
```

**Usage (Windows):**
```cmd
isl.bat transform deploy.isl ^
  -p environment=production ^
  -p version=2.1.5 ^
  -p replicas=3 ^
  -p "features={\"monitoring\":true,\"logging\":true,\"metrics\":true}" ^
  -p "resources={\"cpu\":\"2000m\",\"memory\":\"4Gi\"}" ^
  --pretty
```

### Example 3: Data Enrichment

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
# Combine input file with params
isl transform enrich.isl \
  -i customer-data.json \
  -p region=us-east-1 \
  -p 'tags=["priority","verified"]' \
  -p 'metadata={"source":"api","version":"v2"}' \
  -o enriched-data.json
```

### Example 4: Feature Flags

**feature-flags.isl:**
```isl
fun run($enableLogging, $enableMetrics, $enableCache, $cacheConfig) {
    config: {
        features: {
            logging: $enableLogging,
            metrics: $enableMetrics,
            cache: $enableCache
        },
        cacheSettings: $cacheConfig
    }
}
```

**Usage:**
```bash
isl transform feature-flags.isl \
  -p enableLogging=true \
  -p enableMetrics=true \
  -p enableCache=true \
  -p 'cacheConfig={"ttl":3600,"maxSize":1000}' \
  --pretty
```

### Example 5: Conditional Processing

**process.isl:**
```isl
fun run($input, $isDryRun, $verbose, $filters) {
    processing: {
        data: $input,
        mode: $isDryRun ? "dry-run" : "execute",
        verboseLogging: $verbose,
        appliedFilters: $filters,
        timestamp: @.Date.Now()
    }
}
```

**Usage:**
```bash
# Production run
isl transform process.isl \
  -i data.json \
  -p isDryRun=false \
  -p verbose=false \
  -p 'filters={"status":"active","type":"premium"}'

# Dry run with verbose logging
isl transform process.isl \
  -i data.json \
  -p isDryRun=true \
  -p verbose=true \
  -p 'filters={"status":"active","type":"premium"}'
```

## Combining Parameters with Other Options

### With Variables File
```bash
# vars.yaml contains common settings
# Command-line params override vars file values
isl transform script.isl \
  -v vars.yaml \
  -p environment=staging \
  -p debug=true
```

**Precedence (highest to lowest):**
1. `--param` (command-line)
2. `--vars` (file)
3. `--input` (file, accessible as `$input`)

### With Input Files
```bash
# Input data is accessible as $input variable
# Additional params add metadata or configuration
isl transform process.isl \
  -i orders.json \
  -p processingDate=2024-11-16 \
  -p batchId=batch-001 \
  -p 'options={"validate":true,"notify":true}'
```

## Tips and Best Practices

1. **Quote JSON values**: Always use quotes for JSON objects/arrays
   - Windows: Double quotes with escaped inner quotes `"config={\"key\":\"value\"}"`
   - Linux/Mac: Single quotes `'config={"key":"value"}'`

2. **Multiple parameters**: Repeat `-p` for each parameter
   ```bash
   -p param1=value1 -p param2=value2 -p param3=value3
   ```

3. **Variable naming**: Parameters become ISL variables with `$` prefix
   ```bash
   -p userName=Alice  # Accessible as $userName in ISL
   ```

4. **Type detection**: The CLI automatically detects types:
   - Strings: `name=Alice`
   - Numbers: `age=30`, `price=19.99`
   - Booleans: `active=true` (lowercase)
   - Null: `optional=null`
   - JSON: `config={"key":"value"}`

5. **Debugging**: Use `--pretty` to see formatted output
   ```bash
   isl transform script.isl -p debug=true --pretty
   ```

6. **Line continuation**:
   - Linux/Mac: Use `\`
   - Windows: Use `^`

## Common Patterns

### Configuration Management
```bash
# Different environments
isl transform deploy.isl -p environment=dev -p replicas=1
isl transform deploy.isl -p environment=prod -p replicas=5
```

### Data Processing Pipelines
```bash
# Stage 1: Extract with params
isl transform extract.isl -i raw.json -p source=api -p timestamp=2024-11-16

# Stage 2: Transform with params
isl transform transform.isl -i extracted.json -p enrichMetadata=true

# Stage 3: Load with params
isl transform load.isl -i transformed.json -p destination=warehouse
```

### Testing Different Scenarios
```bash
# Test case 1
isl transform test.isl -p scenario=success -p 'data={"status":"ok"}'

# Test case 2
isl transform test.isl -p scenario=error -p 'data={"status":"error","code":500}'
```

## Troubleshooting

### JSON Parsing Errors
If JSON params fail to parse:
- Check quote escaping on Windows
- Use single quotes on Linux/Mac
- Validate JSON syntax first

### Parameter Not Found
If ISL can't find a parameter:
- Remember to use `$` prefix in ISL: `$paramName`
- Check parameter name spelling
- Use `--help` to verify syntax

### Shell Interpretation Issues
If the shell interprets special characters:
- Use quotes around the entire value
- Escape special characters appropriately for your shell

