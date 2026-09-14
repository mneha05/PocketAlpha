# PocketAlpha

PocketAlpha is a polished native Android paper-investing prototype. It pairs a Kotlin + Jetpack Compose client with a dependency-free Node.js API backed by SQLite. The project is intentionally scoped as a portfolio/watchlist and paper-trading experience—no real money or brokerage connectivity.

## What works

- Account creation and sign-in with scrypt password hashing and expiring signed sessions
- Market overview, search, quotes, and deterministic price-history charts
- Persistent per-user watchlists
- Paper buy/sell orders with server-side validation, cash balances, positions, and order history
- Loading, empty, error, and retry states in the Android client
- SQLite persistence and transactional order execution
- Responsive edge-to-edge Compose UI with a dark, original visual system
- Backend integration tests using Node's built-in test runner
- GitHub Actions CI and Railway health-check configuration

## Run the backend

Requires Node.js 22.5 or newer (the project is tested with Node 24).

```bash
cd backend
npm start
```

The API starts at `http://localhost:8080`. On an Android emulator, the app reaches it at `http://10.0.2.2:8080/api/`. To use a physical device, update `BuildConfig.API_BASE_URL` in `android/app/build.gradle.kts` to your computer's LAN address.

For any deployment, set a strong `AUTH_SECRET` and mount a persistent path for `DB_PATH`; see `backend/.env.example`. A dependency-free Dockerfile is included as well.

Demo login: `demo@pocketalpha.app` / `DemoPass123!`

## Run the Android app

1. Open the `android` folder in Android Studio.
2. Allow Gradle sync to finish.
3. Start the backend.
4. Run the `app` configuration on an emulator using API 26 or newer.

The Android build uses AGP 9.4, Gradle 9.6, Kotlin 2.3, and the Compose BOM. Android Studio can install the matching Gradle distribution during first sync.

## Test

```bash
cd backend
npm test
```

## Architecture

```text
Jetpack Compose UI -> AppViewModel -> MarketRepository -> ApiClient
                                                       -> REST API
REST API -> Auth / validation -> SQLite -> deterministic market simulator
```

## Interview-safe project description

> I built PocketAlpha, a native Android paper-investing app in Kotlin and Jetpack Compose. I implemented an authenticated REST backend with SQLite, server-side order validation, persistent watchlists, quote history, and portfolio accounting. On Android, I built state-driven loading and error flows, custom Canvas charts, search, optimistic-feeling interactions, and reusable design components. I used deterministic demo data so the project runs without a paid market-data key, while keeping the data layer replaceable with a live provider.

## Important note

PocketAlpha is an educational paper-trading project. Prices are simulated and must not be used for financial decisions.
