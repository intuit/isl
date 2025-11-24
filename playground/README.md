# 🎮 ISL Playground

**A full-stack, production-ready playground for ISL (Intuitive Scripting Language)**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9-blue.svg)](https://www.typescriptlang.org/)
[![Monaco](https://img.shields.io/badge/Monaco-4.7-purple.svg)](https://microsoft.github.io/monaco-editor/)

---

## 🚀 Quick Start

**Start the playground in 2 commands:**

```bash
# Terminal 1 - Backend
cd playground/backend && ../../gradlew bootRun

# Terminal 2 - Frontend
cd playground/frontend && npm install && npm run dev
```

Open **http://localhost:3000** and start transforming!

### 🔗 Using URL Parameters

You can also open the playground with pre-loaded code using URL parameters:

```
http://localhost:3000?isl_encoded=BASE64_ISL&input_encoded=BASE64_INPUT
```

This enables "Run in Playground" buttons in documentation. See [PLAYGROUND_URL_FEATURE.md](PLAYGROUND_URL_FEATURE.md) for details.

---

## 📚 Documentation

- **[📖 Quick Start Guide](QUICKSTART.md)** - Get up and running in 5 minutes
- **[🚀 Deployment Guide](DEPLOYMENT.md)** - Deploy to Railway (with options for Vercel)
- **[⚡ Combined Deployment](COMBINED_DEPLOYMENT.md)** - Serve frontend + backend from one service
- **[🔧 Frontend Environment Setup](frontend/ENV_CONFIG.md)** - Configure API URLs
- **[📋 Build Summary](BUILD_SUMMARY.md)** - Complete technical overview
- **[🎮 Playground URL Feature](PLAYGROUND_URL_FEATURE.md)** - "Run in Playground" buttons for docs
- **[Backend README](backend/README.md)** - API documentation
- **[Frontend README](frontend/README.md)** - UI documentation

---

## ✨ Features

### 🎨 Frontend
- **Monaco Editor** with custom ISL syntax highlighting
- **Three-panel layout**: ISL Code | Input JSON | Output
- **Real-time validation** with error line/column info
- **Pre-loaded examples** to get started quickly
- **Responsive design** for desktop and mobile
- **Beautiful gradient UI** with VS Code Dark+ theme
- **URL Parameters Support** - Load code via query strings for "Run in Playground" buttons

### 🔧 Backend
- **Spring Boot 3.2 API** with Kotlin
- **ISL transformation engine** integration
- **Detailed error reporting** (parse, compilation, runtime)
- **CORS enabled** for cross-origin requests
- **Health check endpoint** for monitoring
- **Railway-ready** configuration

### 🌈 ISL Language Support
- Custom Monaco language definition
- Syntax highlighting for keywords, variables, functions
- Support for operators (`??`, `->`, `|`, etc.)
- String interpolation and math expressions
- Auto-closing brackets and quotes
- VS Code Dark+ theme colors

---

## 🎯 API Endpoints

```
POST /api/transform  - Execute ISL transformation
POST /api/validate   - Validate ISL syntax
GET  /api/examples   - Get pre-loaded examples
GET  /api/health     - Health check
```

---

## 💡 Example Transformation

Try this in the playground:

**ISL Code:**
```isl
{
  products: $input.items | map({
    title: $.name | upper,
    price: $.price,
    discounted: {{ $.price * 0.9 }}
  })
}
```

**Input:**
```json
{
  "items": [
    {"name": "Widget", "price": 10},
    {"name": "Gadget", "price": 20}
  ]
}
```

**Output:**
```json
{
  "products": [
    {"title": "WIDGET", "price": 10, "discounted": 9.0},
    {"title": "GADGET", "price": 20, "discounted": 18.0}
  ]
}
```

---

## 🌐 Deployment

### Production Deployment (~$5/month)

1. **Backend → Railway** ($5/month)
   - Auto-deploys from GitHub
   - Includes build configuration
   - See [DEPLOYMENT.md](DEPLOYMENT.md)

2. **Frontend → Vercel** (Free)
   - Auto-deploys from GitHub
   - Set `VITE_API_URL` environment variable
   - See [DEPLOYMENT.md](DEPLOYMENT.md)

---

## 📦 Tech Stack

| Layer    | Technology      | Version | Purpose                |
|----------|----------------|---------|------------------------|
| Backend  | Spring Boot    | 3.2     | API Framework          |
| Backend  | Kotlin         | 1.9     | Language               |
| Backend  | ISL Transform  | 2.4     | Transformation Engine  |
| Frontend | React          | 19      | UI Framework           |
| Frontend | TypeScript     | 5.9     | Type Safety            |
| Frontend | Vite           | 7.2     | Build Tool             |
| Frontend | Monaco Editor  | 4.7     | Code Editor            |
| Frontend | Axios          | 1.13    | HTTP Client            |

---

## 📁 Project Structure

```
playground/
├── backend/                    # Spring Boot API
│   ├── src/main/kotlin/...     # Kotlin source
│   ├── build.gradle.kts        # Build config
│   └── railway.json            # Railway config
│
├── frontend/                   # React SPA
│   ├── src/
│   │   ├── App.tsx             # Main component
│   │   ├── isl-language.ts     # Monaco ISL syntax
│   │   └── ...
│   ├── package.json
│   └── vite.config.ts
│
├── README.md                   # This file
├── QUICKSTART.md               # Getting started
├── DEPLOYMENT.md               # Production deploy
├── BUILD_SUMMARY.md            # Technical details
├── start-backend.sh/.bat       # Startup scripts
└── start-frontend.sh/.bat      # Startup scripts
```

---

## 🎓 Learn ISL

- **[ISL Documentation](https://intuit.github.io/isl)** - Complete language reference
- **[Quick Start](https://intuit.github.io/isl/quickstart/)** - 5-minute tutorial
- **[Examples](https://intuit.github.io/isl/examples/)** - Common transformations
- **[Built-in Modifiers](https://intuit.github.io/isl/language/modifiers/)** - Function reference

---

## 🛠️ Development

### Prerequisites
- Java 17+
- Node.js 18+
- Gradle 8+ (wrapper included)

### Run Locally
```bash
# Backend (port 8080)
cd playground/backend
../../gradlew bootRun

# Frontend (port 3000)
cd playground/frontend
npm install
npm run dev
```

### Build for Production
```bash
# Backend
cd playground/backend
../../gradlew build

# Frontend
cd playground/frontend
npm run build
```

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| Backend won't start | Check Java version: `java -version` (need 17+) |
| Frontend won't start | Check Node version: `node -version` (need 18+) |
| Port 8080 in use | Kill process or change port in `application.yml` |
| Port 3000 in use | Change port in `vite.config.ts` |
| Can't connect to API | Check CORS settings in `WebConfig.kt` |
| Build fails | Run `../../gradlew clean build` or `npm clean-install` |

---

## 📈 Features Roadmap

- [ ] Syntax auto-completion
- [ ] Transformation history
- [ ] Save/load transformations
- [ ] Share transformations via URL
- [ ] File upload for input JSON
- [ ] Download output as file
- [ ] Dark/light theme toggle
- [ ] More code examples
- [ ] Performance metrics
- [ ] Collaboration features

---

## 🤝 Contributing

Contributions welcome! Areas for improvement:

- Additional ISL examples
- UI/UX enhancements
- Performance optimizations
- Documentation improvements
- Bug fixes

---

## 📄 License

Same as [ISL project](https://github.com/intuit/isl)

---

## 🔗 Links

- **ISL Documentation:** https://intuit.github.io/isl
- **ISL GitHub:** https://github.com/intuit/isl
- **Railway:** https://railway.app
- **Vercel:** https://vercel.com

---

## 💬 Support

- **Documentation:** See [`QUICKSTART.md`](QUICKSTART.md) and [`DEPLOYMENT.md`](DEPLOYMENT.md)
- **ISL Language:** https://intuit.github.io/isl
- **Issues:** Open an issue on GitHub

---

## 🎉 Get Started Now!

```bash
# Clone/navigate to project
cd playground

# Start backend (Terminal 1)
./start-backend.sh  # or start-backend.bat on Windows

# Start frontend (Terminal 2)
./start-frontend.sh  # or start-frontend.bat on Windows

# Open browser
open http://localhost:3000
```

**Start transforming with ISL!** 🚀

---

**Built with ❤️ for the ISL community**

