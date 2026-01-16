# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Build, run, and test

This is a single-module Android app (`:app`) built with Gradle and Kotlin.

### Core Gradle commands
- Build debug APK:
  - `./gradlew assembleDebug`
- Clean build outputs:
  - `./gradlew clean`

### Lint
- Run Android lint on the app module:
  - `./gradlew :app:lint`

### Unit tests (local JVM)
- Run all unit tests for the app module:
  - `./gradlew :app:testDebugUnitTest`
- Run a single unit test method (example):
  - `./gradlew :app:testDebugUnitTest --tests "com.example.aisummarize.ExampleUnitTest.addition_isCorrect"`

### Instrumentation tests (on device/emulator)
- Run all connected Android tests:
  - `./gradlew :app:connectedDebugAndroidTest`

> Note: On Windows PowerShell, `./gradlew` will invoke `gradlew.bat` in this repo.

## High-level architecture

### Modules and entry points
- **Modules**: Single Android application module `:app`.
- **Launcher activity**: `MainActivity` (declared as `MAIN`/`LAUNCHER` in `AndroidManifest.xml`) hosts the Compose-based summary list UI.
- **Share target**: `LinkSummaryActivity` is exported with an `ACTION_SEND` / `text/plain` intent-filter to accept shared links from other apps.
- **Screen capture flow**:
  - `QSTileService` exposes a Quick Settings tile that launches `ScreenCaptureActivity`.
  - `ScreenCaptureActivity` requests screen-capture permission via `MediaProjectionManager`, starts `MediaProjectionService` as a foreground service, and captures a single frame for OCR.

All of these components are registered in `app/src/main/AndroidManifest.xml` alongside required permissions (internet, foreground service for media projection, notifications, etc.).

### Data and persistence layer
- **Room database**:
  - `AppDatabase` is a singleton Room database (`summaries`), configured with `fallbackToDestructiveMigration`.
  - `SummaryItem` is the single `@Entity` representing a stored summary record:
    - Fields: `id` (PK, auto-generated), `type` (`"link"` / `"screenshot"`), `link`, `summary`, `title`, `timestamp`.
  - `SummaryDao` provides:
    - `insertSummary` (suspend) returning the inserted row ID.
    - `getAllSummaries()` and `searchSummaries(query)` returning `LiveData<List<SummaryItem>>`, ordered newest-first.
    - `getSummaryById`, `deleteSummaryById`, and `deleteAllSummaries` for targeted / bulk operations.

### ViewModel and state management
- **`SummaryViewModel` (in `app/src/main/java/models`)**:
  - Extends `AndroidViewModel` and owns the `SummaryDao` instance from `AppDatabase`.
  - Maintains a `MutableStateFlow<String>` search query.
  - Exposes `summaries: LiveData<List<SummaryItem>>` by combining:
    - `dao.getAllSummaries()` when the query is empty.
    - `dao.searchSummaries(query)` when the query is non-empty.
  - Provides `updateSearchQuery(query: String)` and `deleteById(id: Int)` APIs used by the UI.
- **`SummaryViewModelFactory`**: standard `ViewModelProvider.Factory` that instantiates `SummaryViewModel` with the `Application` context, used from composables via `viewModel(factory = ...)`.

### UI layer (Compose + Activities)
- **Main list UI**:
  - `MainActivity` initializes `AppDatabase` (also exposed via a `database` companion property) and sets a Compose `Scaffold` whose content is `SummaryListFromDb`.
  - `SummaryListFromDb`:
    - Obtains an `Application` from `LocalContext`.
    - Creates a `SummaryViewModel` via `SummaryViewModelFactory`.
    - Holds a local `searchText` state and passes updates to `viewModel.updateSearchQuery`.
    - Observes `viewModel.summaries` via `observeAsState` and passes the list into `SummaryList`.
  - `SummaryList` renders a `LazyColumn` of `SummaryUiItem` cards, keyed by `SummaryItem.id`.
- **Summary item UI**:
  - `SummaryUiItem` wraps each card in a `SwipeToDismiss`.
    - Swiping end-to-start calls `viewModel.deleteById(summaryItem.id)` via a locally created `SummaryViewModel` (using the same factory pattern).
  - Shows:
    - A formatted timestamp (`rememberFormattedTimestamp`).
    - A bold title.
    - A summary snippet (collapsible/expandable with chevron icon).
    - When expanded, the `link` is displayed as an underlined, clickable URL (opens via `Intent.ACTION_VIEW`).
- **Individual summary screen**:
  - `SummaryActivity` expects an `EXTRA_SUMMARY_ID` integer extra.
  - In `onCreate`, it:
    - Validates the ID.
    - Fetches `SummaryItem` via `AppDatabase.getDatabase(...).summaryDao().getSummaryById(id)` on a coroutine.
    - If found, sets a Compose `SummaryScreen` showing the full summary and a clickable link.
  - `SummaryScreen` also exposes a "Go to App" button that navigates back to `MainActivity` and finishes the current activity.
- **Loading UI**:
  - `SummarizeLoadingScreen` is a simple composable that shows a `CircularProgressIndicator` and a status message, used while link summaries are being generated in `LinkSummaryActivity`.

### AI summarization flows
- **Link summarization (`LinkSummaryActivity`)**:
  - Reads an incoming `ACTION_SEND` `text/plain` intent and extracts `Intent.EXTRA_TEXT`.
  - Validates that the shared text is an HTTP(S) URL.
  - Attempts to fetch HTML using Jsoup, preferring `<article>` text and falling back to `body().text()`.
  - Chooses between two prompt strategies:
    - If extracted article text is short, asks the model to read and summarize directly from the URL.
    - Otherwise, passes the extracted article text into the prompt.
  - Uses the Firebase AI Logic SDK (`Firebase.ai`) with the `GenerativeBackend.googleAI()` backend and the `gemini-2.5-flash-lite` model to:
    - Generate the main summary content.
    - Generate a separate short title (3–5 words) using a strict "single-result, no explanation" prompt.
  - Persists the result as a `SummaryItem` with `type = "link"` and then launches `SummaryActivity` with `EXTRA_SUMMARY_ID` set to the inserted row ID.
- **Screen capture summarization (`ScreenCaptureActivity` + services)**:
  - `ScreenCaptureActivity` drives the screen capture flow:
    - Requests a one-time `MediaProjection` permission.
    - Starts `MediaProjectionService` in the foreground to comply with media projection requirements.
    - Creates an `ImageReader`-backed `VirtualDisplay`, captures a single frame after a short delay, and converts it to a `Bitmap`.
    - Pipes the bitmap through ML Kit Text Recognition (`TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)`) to obtain raw on-screen text.
    - Sends the recognized text to the same `gemini-2.5-flash-lite` generative model (via `Firebase.ai` / `GenerativeBackend.googleAI()`) with a prompt asking for a concise summary of key points.
  - The summarization result is currently passed to `SummaryActivity` via an `EXTRA_SUMMARY` string extra rather than being saved in Room like link-based summaries; be aware of this divergence if you unify or refactor the summary display logic.
  - `MediaProjectionService` runs as a foreground service (channel `media_projection_service_channel`) to keep the projection alive and shows a minimal ongoing notification.
  - `QSTileService`:
    - Warms up ML Kit by pre-loading a tiny `InputImage` when the tile is added.
    - On tile click (API level permitting), uses `startActivityAndCollapse` with a `PendingIntent` to launch `ScreenCaptureActivity`.

### Theming and resources
- Compose theming files live under `app/src/main/java/com/example/aisummarize/ui/theme` and `app/src/main/res/values/themes.xml`.
- Layout and resource files (colors, strings, icons) are in the usual `res/` subdirectories. The main feature UIs are implemented in Compose rather than XML layouts.
