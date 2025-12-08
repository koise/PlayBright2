# PlayBright Mobile - Android App

Android application for PlayBright learning platform built with Kotlin and MVVM architecture.

## Architecture

### MVVM (Model-View-ViewModel)

```
├── data/
│   ├── model/          # Data models
│   │   └── User.kt
│   └── repository/     # Data repositories
│       └── AuthRepository.kt
├── ui/
│   ├── activity/       # Activities (Views)
│   │   ├── SplashActivity.kt
│   │   ├── LoginActivity.kt
│   │   ├── SignUpActivity.kt
│   │   └── MainActivity.kt
│   └── viewmodel/      # ViewModels
│       └── AuthViewModel.kt
```

## Features

- ✅ Firebase Authentication
  - Email/Password login
  - Google Sign-In
  - User registration
- ✅ MVVM Architecture
- ✅ Kotlin Coroutines
- ✅ LiveData for reactive UI
- ✅ ViewBinding
- ✅ Material Design 3

## Setup

### 1. Prerequisites

- Android Studio Hedgehog or newer
- JDK 11 or higher
- Android SDK 31+

### 2. Firebase Configuration

The `google-services.json` file is already configured. If you need to update it:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: `playbright-4c1cd`
3. Go to Project Settings → Your apps → Android app
4. Download `google-services.json`
5. Place it in `app/` directory

### 3. Google Sign-In Setup

Update the Web Client ID in `app/src/main/res/values/strings.xml`:

```xml
<string name="default_web_client_id">YOUR_WEB_CLIENT_ID_HERE</string>
```

Get it from:
1. Firebase Console → Authentication → Sign-in method → Google
2. Copy the Web SDK configuration → Web client ID

### 4. Build and Run

```bash
# Open project in Android Studio
# Sync Gradle
# Run on emulator or device
```

Or using command line:

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Project Structure

### Data Layer

**Model (`User.kt`)**
- Represents user data
- Fields: id, email, displayName, photoUrl, role

**Repository (`AuthRepository.kt`)**
- Handles Firebase Authentication
- Methods:
  - `signInWithEmail()`
  - `signInWithGoogle()`
  - `signUp()`
  - `signOut()`
  - `getCurrentUser()`

### ViewModel Layer

**AuthViewModel**
- Manages authentication state
- Exposes LiveData for UI observation
- States: Idle, Loading, Success, Error, SignedOut

### View Layer

**Activities**
- `SplashActivity`: Initial screen, checks auth state
- `LoginActivity`: Email/password and Google sign-in
- `SignUpActivity`: User registration
- `MainActivity`: Home screen after login

## Dependencies

```kotlin
// Firebase
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.android.gms:play-services-auth:20.7.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
```

## Usage

### Sign In with Email

```kotlin
viewModel.signInWithEmail("email@example.com", "password")
```

### Sign In with Google

```kotlin
// Launch Google Sign-In
val signInIntent = googleSignInClient.signInIntent
googleSignInLauncher.launch(signInIntent)

// Handle result
viewModel.signInWithGoogle(account)
```

### Observe Auth State

```kotlin
viewModel.authState.observe(this) { state ->
    when (state) {
        is AuthViewModel.AuthState.Loading -> showLoading()
        is AuthViewModel.AuthState.Success -> navigateToHome()
        is AuthViewModel.AuthState.Error -> showError(state.message)
        else -> {}
    }
}
```

## Testing

Run tests:

```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Troubleshooting

### Google Sign-In not working

1. Check SHA-1 certificate fingerprint is added to Firebase
2. Verify Web Client ID in `strings.xml`
3. Enable Google Sign-In in Firebase Console

### Build errors

```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

## Next Steps

- [ ] Add more learning modules
- [ ] Implement offline support
- [ ] Add user profile management
- [ ] Integrate with backend API
- [ ] Add unit tests
- [ ] Add UI tests

## License

Proprietary - PlayBright Learning Platform

