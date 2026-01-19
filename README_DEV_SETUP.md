# PlayBright Development Setup Guide

## Quick Start

### 1. Install Dependencies
```bash
npm install
```

### 2. Start Development Servers
```bash
npm run dev
```

This will start both:
- **Backend** on `http://localhost:3000`
- **Frontend** on `http://localhost:5174`

## Firebase Configuration

### Required: Firebase Service Account

The backend requires a Firebase service account file to connect to Firestore.

**Option 1: Service Account File (Recommended)**
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select project: `playbright-830a5`
3. Go to Project Settings → Service Accounts
4. Click "Generate New Private Key"
5. Save the JSON file in `PlayBrightAdmin/backend/`
6. The file should be named: `playbright-830a5-firebase-adminsdk-*.json`

**Option 2: Environment Variable**
Create `.env` file in `PlayBrightAdmin/backend/`:
```
FIREBASE_SERVICE_ACCOUNT='{"type":"service_account",...}'
```

## Verify Setup

### Check Backend
```bash
curl http://localhost:3000
```
Should return: "Backend (Firebase Mode) is running!"

### Check Frontend
Open: `http://localhost:5174`

## Troubleshooting

### Backend won't start
- Check if port 3000 is available
- Verify Firebase service account file exists
- Check backend logs for errors

### Frontend can't connect to backend
- Make sure backend is running on port 3000
- Check browser console for errors
- Verify Vite proxy configuration

### 500 Error on Login
- Firebase Admin not initialized - add service account file
- Check backend console logs for detailed error
- Verify admin user exists in Firestore

## Creating Admin User

### Method 1: PowerShell Script
```powershell
cd PlayBrightAdmin/backend
npm run create-admin
```

### Method 2: Postman
- Method: POST
- URL: `http://localhost:3000/api/faculty`
- Headers: `Content-Type: application/json`
- Body:
```json
{
  "name": "System Administrator",
  "email": "admin@stacruz.edu.ph",
  "password": "admin123",
  "role": "Admin"
}
```

### Method 3: Direct API Call
```powershell
$body = @{ name = "System Administrator"; email = "admin@stacruz.edu.ph"; password = "admin123"; role = "Admin" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:3000/api/faculty" -Method POST -Body $body -ContentType "application/json"
```

## Default Login Credentials

After creating admin user:
- **Email:** `admin@stacruz.edu.ph`
- **Password:** `admin123`

## Project Structure

```
PlayBright/
├── PlayBrightAdmin/
│   ├── backend/          # Express.js backend (port 3000)
│   │   ├── config/       # Firebase config
│   │   ├── controllers/  # Route controllers
│   │   ├── models/       # Data models
│   │   └── routes/       # API routes
│   └── frontend/         # React + Vite frontend (port 5174)
│       └── src/
│           ├── config/   # API & Firebase config
│           ├── pages/    # React pages
│           └── components/
└── package.json          # Root workspace config
```

## Development Scripts

- `npm run dev` - Start both backend and frontend
- `npm run dev:backend` - Start only backend
- `npm run dev:frontend` - Start only frontend
- `npm run create-admin` - Create admin user (requires backend running)

## Ports

- **3000** - Backend API server
- **5174** - Frontend Vite dev server

## Environment Variables

### Backend (.env in PlayBrightAdmin/backend/)

**Development:**
```
PORT=3000
NODE_ENV=development
CLIENT_URL=http://localhost:5174
FIREBASE_PROJECT_ID=playbright-830a5
STORAGE_BUCKET=playbright-830a5.firebasestorage.app
FIREBASE_SERVICE_ACCOUNT='{...}'  # Optional, if not using file
```

**Production:**
```
PORT=3000
NODE_ENV=production
CLIENT_URL=https://playbright.vercel.app
ALLOWED_ORIGINS=https://playbright.vercel.app
FIREBASE_PROJECT_ID=playbright-830a5
STORAGE_BUCKET=playbright-830a5.firebasestorage.app
FIREBASE_SERVICE_ACCOUNT='{...}'  # Required for production
```

### Frontend (.env in PlayBrightAdmin/frontend/)

**Development:**
```
VITE_BACKEND_URL=http://localhost:3000
VITE_FIREBASE_API_KEY=...
VITE_FIREBASE_PROJECT_ID=playbright-830a5
VITE_FIREBASE_AUTH_DOMAIN=playbright-830a5.firebaseapp.com
VITE_FIREBASE_STORAGE_BUCKET=playbright-830a5.firebasestorage.app
```

**Production:**
```
# API URLs are auto-configured to use production domain (https://playbright.vercel.app)
# Only set VITE_BACKEND_URL if backend is on a different domain
VITE_FIREBASE_API_KEY=...
VITE_FIREBASE_PROJECT_ID=playbright-830a5
VITE_FIREBASE_AUTH_DOMAIN=playbright-830a5.firebaseapp.com
VITE_FIREBASE_STORAGE_BUCKET=playbright-830a5.firebasestorage.app
```

## Production Domain

The main production domain is: **https://playbright.vercel.app**

Domain configuration is centralized in:
- Backend: `PlayBrightAdmin/backend/config/domainConfig.js`
- Frontend: `PlayBrightAdmin/frontend/src/config/domainConfig.js`

For detailed domain configuration information, see [DOMAIN_CONFIGURATION.md](./DOMAIN_CONFIGURATION.md).
