# ISL Playground Backend

Spring Boot backend API for the ISL Playground.

## API Endpoints

### POST /api/transform
Execute an ISL transformation.

**Request:**
```json
{
  "isl": "{ result: $input.value }",
  "input": "{ \"value\": \"test\" }"
}
```

**Response:**
```json
{
  "success": true,
  "output": "{ \"result\": \"test\" }"
}
```

### POST /api/validate
Validate ISL syntax.

**Request:**
```json
{
  "isl": "{ result: $input.value }"
}
```

**Response:**
```json
{
  "valid": true,
  "errors": []
}
```

### GET /api/examples
Get example ISL transformations.

## Local Development

```bash
./gradlew bootRun
```

## Build

```bash
./gradlew build
```

## Railway Deployment

This app is configured to deploy on Railway. Push to GitHub and connect your repository in Railway.

Environment variables:
- `PORT`: Auto-configured by Railway

