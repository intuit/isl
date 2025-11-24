# ISL Playground Frontend

React + TypeScript + Vite frontend for the ISL Playground.

## Features

- **Monaco Editor** with ISL syntax highlighting
- **Live transformation** - Run ISL code against JSON input
- **Validation** - Check ISL syntax before running
- **Examples** - Pre-loaded examples to get started
- **Error highlighting** - Clear error messages with line/column info

## Development

```bash
npm install
npm run dev
```

The app will run on `http://localhost:3000`

## Build

```bash
npm run build
```

## Environment Variables

- `VITE_API_URL` - Backend API URL (default: `http://localhost:8080/api`)

## Deployment

This frontend can be deployed to:
- Vercel
- Netlify
- Railway (static hosting)
- Any static hosting service

Make sure to set the `VITE_API_URL` environment variable to your backend URL.
