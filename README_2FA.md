# Weather Check Android App

A clean Android weather app with account login, optional OTP verification, and personal history/favorites.

## Highlights

- Search current weather by city
- Fetch weather by current location
- Save and manage favorites
- View recent search history
- Login with `username + password`
- Post-login choice:
  - `אימות` (email OTP verification)
  - `המשך` (continue directly)

## Authentication Flow

```text
LoginActivity
  -> POST /api/auth/login  { username, password }
  <- { loginOk, username, email, otpToken }

AuthMethodSelectionActivity
  - shows email from server response (read-only)
  - button: אימות  -> POST /api/auth/send-otp -> OTPVerificationActivity
  - button: המשך   -> POST /api/auth/dev-login -> HomeActivity

OTPVerificationActivity
  -> POST /api/auth/verify-otp { otpToken, code }
  <- { token, username }
  -> HomeActivity
```

## API Contract (App Side)

Base URL and endpoints are defined in `app/src/main/java/com/example/weather_check/ApiConfig.kt`.

Auth endpoints used by the app:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/send-otp`
- `POST /api/auth/verify-otp`
- `POST /api/auth/dev-login`

Weather/data endpoints:

- `GET /api/weather`
- `GET /api/history`
- `GET /api/favorites`
- plus related delete/management routes

## Important Notes

- Login screen does **not** ask for email.
- Email is received from the server in login response.
- For send-otp calls, app sends:
  - `Authorization: Bearer <otpToken>`
  - body `{ "otpToken": "..." }`
- If OTP email sending fails with messages like `535 5.7.8` or `email service authentication failed`, the issue is on server email provider authentication, not Android UI flow.

## Quick Start

### 1) Configure server URL

Edit `app/src/main/java/com/example/weather_check/ApiConfig.kt`:

```kotlin
const val BASE_URL = BASE_URL_ANDROID_EMULATOR // emulator (10.0.2.2)
// const val BASE_URL = BASE_URL_PHONE
// const val BASE_URL = BASE_URL_PRODUCTION
```

### 2) Run backend

Ensure backend is up and auth endpoints are reachable.

### 3) Build app

```bash
./gradlew :app:assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Project Structure (Main)

- `app/src/main/java/com/example/weather_check/`
  - `LoginActivity.kt`
  - `AuthMethodSelectionActivity.kt`
  - `OTPVerificationActivity.kt`
  - `HomeActivity.kt`
  - `ApiConfig.kt`
  - `utils/TokenManager.kt`
- `app/src/main/res/layout/`
  - `activity_login.xml`
  - `activity_auth_method_selection.xml`
  - `activity_otp_verification.xml`
  - `activity_home.xml`

## Troubleshooting

### Login succeeds but OTP send fails

Check backend mail configuration:

- SMTP/API user and password
- app password (if provider requires it)
- host/port/TLS mode
- env vars loaded in deployment
- sender identity permissions

### 404 on auth requests

Verify route prefix matches app config:

- app expects `/api/auth/*`

### Network errors on emulator

Use emulator base URL:

- `http://10.0.2.2:3000`

## Tech

- Kotlin + AndroidX
- OkHttp
- Gson
- Google Location Services

## Status

Core app flow is implemented and buildable. Main integration dependency is server email-service authentication for OTP delivery.
