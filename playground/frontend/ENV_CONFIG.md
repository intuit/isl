# Frontend Environment Configuration

## Overview

The frontend uses environment variables to configure the API URL. Create the appropriate `.env` file based on your deployment scenario.

## Environment Files

### For Combined Deployment (Recommended)

Create `.env.production`:
```bash
# Production - frontend and backend served from same Spring Boot app
VITE_API_URL=/api
```

This uses a relative path since both frontend and backend are on the same domain.

### For Local Development

Create `.env.development`:
```bash
# Development - frontend on Vite dev server, backend on Spring Boot
VITE_API_URL=http://localhost:8080/api
```

### For Separate Deployment

Create `.env.production`:
```bash
# Separate deployment - frontend on Vercel/Netlify, backend on Railway
VITE_API_URL=https://your-backend.railway.app/api
```

Replace `your-backend.railway.app` with your actual backend URL.

## Build Process

### Combined Deployment
When building with the backend, the frontend is automatically:
1. Built via `npm install && npm run build`
2. Copied to Spring Boot's `src/main/resources/static/`
3. Bundled into the Spring Boot JAR

### Separate Deployment
Build the frontend independently:
```bash
npm install
npm run build
```

The `dist/` folder can then be deployed to Vercel, Netlify, or any static hosting service.

## Testing Locally

### Combined Mode (Test the full stack)
```bash
# From playground/backend
./gradlew clean build
java -jar build/libs/isl-playground-1.0.0.jar

# Open browser to http://localhost:8080
```

### Development Mode (Hot reload)
```bash
# Terminal 1 - Backend
cd playground/backend
../../gradlew bootRun

# Terminal 2 - Frontend
cd playground/frontend
npm run dev

# Frontend: http://localhost:5173
# Backend: http://localhost:8080
```

