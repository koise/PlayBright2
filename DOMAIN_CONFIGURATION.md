# Domain Configuration Guide

This document describes how domain configuration works in PlayBright, including production domain setup.

## Production Domain

The main production domain for PlayBright is: **https://playbright.vercel.app**

## Architecture Overview

The application uses centralized domain configuration to manage:
- CORS (Cross-Origin Resource Sharing) settings
- Cookie domain and security settings
- API URL configuration for frontend
- Environment-based URL routing

## Backend Configuration

### Domain Configuration Module

The backend uses `PlayBrightAdmin/backend/config/domainConfig.js` to manage domain settings:

- **Production Domain**: `https://playbright.vercel.app`
- **Development Domains**: `http://localhost:5174`, `http://localhost:3000`, etc.

### CORS Configuration

The backend automatically allows requests from:
- Production: `https://playbright.vercel.app` (and any domains in `ALLOWED_ORIGINS`)
- Development: `http://localhost:5174`, `http://localhost:3000`, etc.

### Environment Variables (Backend)

Create `.env` file in `PlayBrightAdmin/backend/`:

**Development:**
```env
PORT=3000
NODE_ENV=development
CLIENT_URL=http://localhost:5174
FIREBASE_PROJECT_ID=playbright-830a5
STORAGE_BUCKET=playbright-830a5.firebasestorage.app
```

**Production:**
```env
PORT=3000
NODE_ENV=production
CLIENT_URL=https://playbright.vercel.app
ALLOWED_ORIGINS=https://playbright.vercel.app
FIREBASE_PROJECT_ID=playbright-830a5
STORAGE_BUCKET=playbright-830a5.firebasestorage.app
FIREBASE_SERVICE_ACCOUNT='{"type":"service_account",...}'
```

### Cookie Configuration

Cookies are automatically configured based on environment:
- **Development**: `secure: false`, `sameSite: 'lax'`
- **Production**: `secure: true`, `sameSite: 'lax'`

The cookie domain is left undefined to allow cookies to work with the exact domain that sets them (important for Vercel deployments).

## Frontend Configuration

### Domain Configuration Module

The frontend uses `PlayBrightAdmin/frontend/src/config/domainConfig.js` to manage API URLs:

- Automatically detects production vs development environment
- Uses production domain (`https://playbright.vercel.app`) in production
- Uses relative URLs in development (leveraging Vite proxy)
- Falls back to environment variables if set

### Environment Variables (Frontend)

Create `.env` file in `PlayBrightAdmin/frontend/`:

**Development:**
```env
VITE_BACKEND_URL=http://localhost:3000
VITE_FIREBASE_API_KEY=your_api_key
VITE_FIREBASE_PROJECT_ID=playbright-830a5
VITE_FIREBASE_AUTH_DOMAIN=playbright-830a5.firebaseapp.com
VITE_FIREBASE_STORAGE_BUCKET=playbright-830a5.firebasestorage.app
VITE_FIREBASE_MESSAGING_SENDER_ID=your_sender_id
VITE_FIREBASE_APP_ID=your_app_id
VITE_FIREBASE_MEASUREMENT_ID=your_measurement_id
```

**Production:**
```env
# In production, API URLs are auto-configured to use production domain
# Only set these if your backend is on a different domain
# VITE_BACKEND_URL=https://your-backend-api.com
# VITE_API_BASE_URL=https://your-backend-api.com/api
# VITE_AUTH_API_URL=https://your-backend-api.com/auth
# VITE_UPLOAD_API_URL=https://your-backend-api.com/api/upload

VITE_FIREBASE_API_KEY=your_api_key
VITE_FIREBASE_PROJECT_ID=playbright-830a5
VITE_FIREBASE_AUTH_DOMAIN=playbright-830a5.firebaseapp.com
VITE_FIREBASE_STORAGE_BUCKET=playbright-830a5.firebasestorage.app
VITE_FIREBASE_MESSAGING_SENDER_ID=your_sender_id
VITE_FIREBASE_APP_ID=your_app_id
VITE_FIREBASE_MEASUREMENT_ID=your_measurement_id
```

## Deployment

### Vercel Deployment

When deploying to Vercel:

1. **Backend**: Set environment variables in Vercel dashboard:
   - `NODE_ENV=production`
   - `CLIENT_URL=https://playbright.vercel.app`
   - `FIREBASE_SERVICE_ACCOUNT` (JSON string)
   - Other Firebase config variables

2. **Frontend**: Set environment variables in Vercel dashboard:
   - All `VITE_*` variables
   - The frontend will automatically use `https://playbright.vercel.app` for API calls in production

### Firebase Authorized Domains

Make sure to add `playbright.vercel.app` to your Firebase project's authorized domains:
1. Go to Firebase Console
2. Authentication → Settings → Authorized domains
3. Add `playbright.vercel.app`

## Adding Additional Domains

To add additional allowed domains:

1. **Backend**: Add to `ALLOWED_ORIGINS` environment variable (comma-separated):
   ```env
   ALLOWED_ORIGINS=https://playbright.vercel.app,https://www.playbright.vercel.app
   ```

2. **Frontend**: If backend is on a different domain, set:
   ```env
   VITE_BACKEND_URL=https://your-backend-domain.com
   VITE_API_BASE_URL=https://your-backend-domain.com/api
   ```

## Testing Domain Configuration

### Development
- Frontend: `http://localhost:5174`
- Backend: `http://localhost:3000`
- Uses Vite proxy for API calls

### Production
- Frontend: `https://playbright.vercel.app`
- Backend: Should be accessible at the same domain or set via environment variables
- Uses absolute URLs for API calls

## Troubleshooting

### CORS Errors
- Verify `CLIENT_URL` matches your frontend domain
- Check `ALLOWED_ORIGINS` includes all necessary domains
- Ensure backend CORS middleware is configured correctly

### Cookie Issues
- In production, ensure `secure: true` is set (handled automatically)
- Verify `sameSite` setting matches your needs
- Check that cookies are being sent with `withCredentials: true` in axios requests

### API Connection Issues
- Check environment variables are set correctly
- Verify backend is running and accessible
- Check browser console for CORS or network errors
- Ensure Firebase authorized domains include your domain
