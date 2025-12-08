# PlayBright - Quick Start Scripts

## 🚀 One-Command Android Setup

Run the mobile app with emulator from anywhere in the project:

### From Project Root:
```powershell
.\scripts\start-mobile-android.ps1
```

### From Mobile/Frontend:
```powershell
npm run start:android
```

### Using Batch File (Windows):
```cmd
scripts\start-mobile-android.bat
```

### Using Node.js (Cross-platform):
```bash
node scripts/start-mobile-android.js
```

## What It Does

The script automatically:
1. ✅ Checks if backend server is running (starts it if not)
2. ✅ Checks if Android emulator is running
3. ✅ Starts Android emulator if needed
4. ✅ Waits for emulator to fully boot
5. ✅ Starts the mobile app with `npm run android`

## Requirements

- Android Studio installed
- At least one Android Virtual Device (AVD) created
- Backend server accessible (or it will start automatically)
- Node.js and npm installed

## Configuration

The app connects to:
- **Android Emulator**: `http://10.0.2.2:3000` (maps to localhost:3000)
- **Backend**: `http://localhost:3000`

All configuration is automatic! No setup needed.

## Files Created

- `scripts/start-mobile-android.ps1` - PowerShell script (Windows, recommended)
- `scripts/start-mobile-android.bat` - Batch script (Windows, simpler)
- `scripts/start-mobile-android.js` - Node.js script (Cross-platform)
- `scripts/README.md` - Detailed documentation

## Quick Test

1. Make sure backend is running (or let script start it)
2. Run: `.\scripts\start-mobile-android.ps1`
3. Wait for emulator to boot
4. App launches automatically!

---

For more details, see `scripts/README.md`

