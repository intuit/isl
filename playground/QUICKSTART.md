# Quick Start: Combined Deployment

## 1. Configure Environment

Create `playground/frontend/.env.production`:
```bash
VITE_API_URL=/api
```

## 2. Test Locally

```bash
cd playground/backend
./gradlew clean build
java -jar build/libs/isl-playground-1.0.0.jar
```

Open browser to `http://localhost:8080`

## 3. Deploy to Railway

```bash
# Commit changes
git add .
git commit -m "Setup combined deployment"
git push

# Railway will automatically:
# 1. Build frontend (npm install && npm run build)
# 2. Copy frontend to Spring Boot static resources
# 3. Build Spring Boot JAR with bundled frontend
# 4. Deploy everything
```

## 4. Access Your App

- **Frontend**: `https://your-app.railway.app/`
- **API**: `https://your-app.railway.app/api/health`

## Railway Configuration

In Railway dashboard, set:
- **Root Directory**: `playground/backend`
- **Build Command**: `./gradlew clean build -x test`
- **Start Command**: `java -jar build/libs/isl-playground-1.0.0.jar`

That's it! 🚀

---

For more details, see:
- `DEPLOYMENT.md` - Full deployment guide
- `COMBINED_DEPLOYMENT.md` - Technical details
- `frontend/ENV_CONFIG.md` - Environment configuration

