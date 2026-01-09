# PaceNote VLA - Mock Testing Setup

This project includes mock implementations for Firebase Authentication and Play Billing to enable local testing without requiring Firebase console setup or Google Play developer account.

## Mock Mode (Current)

The project is currently configured for **mock testing**:

### What's Mocked:
- **Firebase Authentication**: Auto-authenticates as `test@pacenote.local` (Pro user)
- **Play Billing**: Returns `isPro = true` by default
- **AdMob**: Ads are disabled

### Build Configuration:
```kotlin
// In app/build.gradle.kts, feature/auth/build.gradle.kts, feature/monetization/build.gradle.kts
// These plugins are COMMENTED OUT for mock mode:
// id("com.google.gms.google-services")
// id("com.google.firebase.crashlytics")

// These dependencies are COMMENTED OUT:
// implementation(platform("com.google.firebase:firebase-bom:..."))
// implementation("com.google.firebase:firebase-auth-ktx")
// implementation("com.google.android.gms:play-services-auth:...")
// implementation("com.google.android.gms:play-services-ads:...")
// implementation("com.android.billingclient:billing:...")
```

### Mock Modules:
- `MockAuthRepository` - Auto-authenticates test user
- `MockBillingManager` - Returns Pro status by default
- `MockAdManager` - No real ads shown

## Switching to Production

To enable production Firebase and Play Billing:

### 1. Gradle Plugins
Uncomment in:
- `app/build.gradle.kts`
- `feature/auth/build.gradle.kts`

```kotlin
id("com.google.gms.google-services")
id("com.google.firebase.crashlytics")
```

### 2. Firebase Setup
1. Create project at [Firebase Console](https://console.firebase.google.com/)
2. Add Android app with package name `com.pacenote.vla`
3. Download `google-services.json` to `app/`
4. Enable Authentication (Google Sign-In, Email/Password)
5. Enable Crashlytics and Analytics

### 3. Play Billing Setup
1. Create developer account at [Google Play Console](https://play.google.com/console)
2. Add subscription products:
   - `pacenote_vla_subscription_monthly`
   - `pacenote_vla_subscription_yearly`
3. Update `LiveKitConfig` with your server credentials

### 4. LiveKit Setup
1. Get LiveKit Cloud or self-hosted server
2. Update API keys in `feature/livekit/src/main/kotlin/.../di/LiveKitModule.kt`

## Using Mock Mode

In mock mode, the app will:
- Skip the authentication screen and go directly to the main app
- Show Pro features unlocked
- Never display ads

### Testing Different States

```kotlin
// In MockBillingManager, you can toggle Pro status:
billingManager.toggleProStatus()  // Toggle true/false
billingManager.setProStatus(false)  // Set specific state
```

## File Structure

```
app/src/main/kotlin/com/pacenote/vla/
├── di/
│   ├── AppModule.kt              # Real DI module
│   └── MockDependencies.kt       # Mock DI bindings
└── ...
feature/auth/src/main/kotlin/.../auth/
├── repository/
│   ├── AuthRepository.kt         # Real Firebase impl
│   └── MockAuthRepository.kt     # Mock impl
└── di/
    ├── AuthModule.kt             # Real DI
    └── MockAuthModule.kt         # Mock DI

feature/monetization/src/main/kotlin/.../monetization/
├── billing/
│   ├── BillingManager.kt         # Real Play Billing impl
│   └── MockBillingManager.kt     # Mock impl
└── ads/
    ├── AdManager.kt              # Real AdMob impl
    └── MockAdManager.kt          # Mock impl
```

## Troubleshooting

**Build fails with "unresolved reference: FirebaseUser"**
→ Ensure `firebase-auth-ktx` dependency is present (even in mock mode, for types)

**Google Services plugin error**
→ Ensure `google-services.json` exists OR comment out the plugin
