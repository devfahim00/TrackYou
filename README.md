# TrackYou

Modern offline-first expense tracker for Android (Kotlin + Jetpack Compose + Material 3 + Room).

## Features

- **Home dashboard** - gradient balance card with hide/show toggle, this-month income & expense, spending trend vs last month, quick actions (add expense / income / export), recent transactions
- **History** - month-by-month navigation with chevrons, transactions grouped by day (Today / Yesterday / date) with daily net totals, search by note/category, income/expense filters, swipe-to-delete with Undo, tap-to-edit, date picker when adding/editing
- **Stats** - expense breakdown donut chart by category, top-categories with progress bars, last-6-months income vs expense bar chart, month selector
- **Dena-Paona** - lent & borrowed money with partial payments, settle tracking, person avatars, paona/dena filters
- **Savings goals** - deposit/withdraw, animated progress, targets
- **Settings** - profile, currency, theme mode (System / Light / Dark), CSV export & share
- Fully offline - no account, no internet, data stays on device
- Light & dark theme, edge-to-edge UI, modern adaptive icon (themed icons on Android 13+)

## Tech

- Kotlin 2.0, Jetpack Compose (BOM 2024.09), Material 3
- Room (transactions / debts / goals), ViewModel + StateFlow
- Custom Canvas charts (donut + bars), no chart library
- Package: `com.devfahim00.trackyou`

## Build

Debug APK is built by GitHub Actions on every push to `main` and published under **Releases -> debug-latest**.
