# Combined Deployment Setup

## What Changed

We've configured the ISL Playground to support **combined deployment** where both the frontend and backend are served from a single Spring Boot application on Railway.

## How It Works

### Build Process
1. **Gradle builds the frontend** first:
   - Runs `npm install && npm run build` in `playground/frontend`
   - Generates static files in `frontend/dist/`

2. **Copies frontend to Spring Boot**:
   - Static files are copied to `backend/src/main/resources/static/`
   - These are bundled into the Spring Boot JAR

3. **Spring Boot serves both**:
   - Frontend at `/` (root) - the React SPA
   - API at `/api/*` - the REST endpoints

### Routing
- **SpaConfig.kt**: Handles client-side routing by forwarding non-API requests to `index.html`
- **WebConfig.kt**: Configures CORS for the API
- **API URLs**: All API endpoints are under `/api` prefix

### Static File Optimization
- **Compression**: Enabled for HTML, CSS, JS, JSON
- **Caching**: 1-year cache for static assets with content-based cache busting
- **Resource chain**: Optimized resource handling

## Files Added/Modified

### New Files
- `backend/src/main/kotlin/com/intuit/isl/playground/config/SpaConfig.kt`
- `frontend/ENV_CONFIG.md`

### Modified Files
- `backend/build.gradle.kts` - Added frontend build tasks
- `backend/src/main/resources/application.yml` - Added compression and caching
- `backend/.gitignore` - Ignore generated static files
- `DEPLOYMENT.md` - Added combined deployment option

## Deployment Options

### Option 1: Combined (Recommended)
- **Where**: Single Railway service
- **Cost**: ~$5/month
- **Setup**: Just push to GitHub, Railway auto-builds everything
- **URL**: Everything at `https://your-app.railway.app`

### Option 2: Separate
- **Where**: Backend on Railway, Frontend on Vercel/Netlify
- **Cost**: ~$5/month (Vercel free)
- **Setup**: Two separate deployments
- **URLs**: Different domains, requires CORS

## Testing

### Test Combined Build Locally
```bash
cd playground/backend
./gradlew clean build
java -jar build/libs/isl-playground-1.0.0.jar

# Open http://localhost:8080
```

### Test Development Mode (Hot Reload)
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

## Environment Configuration

Create `.env.production` in `playground/frontend/`:

**For combined deployment:**
```bash
VITE_API_URL=/api
```

**For separate deployment:**
```bash
VITE_API_URL=https://your-backend.railway.app/api
```

See `frontend/ENV_CONFIG.md` for complete details.

## Benefits of Combined Deployment

✅ **Simpler**: One service instead of two  
✅ **Cheaper**: $5/month vs potentially more  
✅ **No CORS issues**: Same origin  
✅ **Single URL**: Easier to share  
✅ **Easier SSL**: One certificate  
✅ **Unified logs**: Everything in one place  

## When to Use Separate Deployment

- Need Vercel's global CDN edge network
- Want frontend-specific analytics/optimization
- Require different scaling for frontend vs backend
- Team prefers specialized hosting for each layer

## Next Steps

1. Create `frontend/.env.production` with `VITE_API_URL=/api`
2. Test locally: `./gradlew clean build && java -jar build/libs/isl-playground-1.0.0.jar`
3. Push to GitHub
4. Railway will automatically deploy
5. Access your app at `https://your-app.railway.app`

