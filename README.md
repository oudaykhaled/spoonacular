# ING Recipes

A multi-module Jetpack Compose Android app built against Spoonacular's `complexSearch` API for the ING coding challenge. Recipes are searchable, sortable, paginated, cacheable offline via Room, and themable through the ING design system. The codebase is organised around clean architecture, convention plugins, and a staff-engineer level test pyramid (unit, instrumentation, macrobenchmark, baseline profile).

## Features

- Recipe search with 350 ms debounce against Spoonacular `complexSearch`.
- Sorting by relevance, popularity, healthiness, price, and time.
- Infinite scroll (manual offset-based paging) and pull-to-refresh.
- Recipe detail screen with ingredients, instructions, and source link.
- Offline cache via Room (previously viewed recipes survive network loss).
- Favorites marked offline-available.
- Material 3 theming driven by an ING tonal palette (Primary40 `#FF6200`).
- In-app theme toggle: System / Light / Dark, persisted in DataStore.
- Material You dynamic color on Android 12+ (behind a user preference).
- Deep link support: `recipes://recipe/<id>`.
- Full Compose UI on Navigation 3 (`rememberNavBackStack` + `NavDisplay`).
- Macrobenchmark module covering cold startup and list scroll, plus a Baseline Profile generator.

## Getting Started

### Prerequisites

- Android Studio Koala (or newer) with AGP 9.1+.
- JDK 17.
- Minimum Android SDK 24, compile/target SDK 36.
- A free Spoonacular API key from [spoonacular.com/food-api](https://spoonacular.com/food-api).

### Setup

1. Clone the repo.
2. Open `ing-challenge/local.properties` (create it if it does not exist) and add:

```bash
SPOONACULAR_API_KEY=<your_key>
```

   The key is read by `core/network/build.gradle.kts` and exposed through `BuildConfig.SPOONACULAR_API_KEY`. You can alternatively export `SPOONACULAR_API_KEY` as an environment variable.

3. Build and install the default dev-debug variant:

```bash
./gradlew :app:installDevDebug
```

Or open the project in Android Studio and run the `app` configuration.

### Variants

The app uses a two-axis variant matrix:

- **Flavor dimension `environment`**: `dev`, `prod`. Both currently point at `https://api.spoonacular.com/`, but the dimension exists so staging/mock URLs can be injected without code changes.
- **Build types**: `debug` (default), `release` (proguard-ready), and `benchmark` (a release-like, debuggable variant declared in `:benchmark` with `matchingFallbacks += "release"` — used only when building against the benchmark module).

Combined variants you will typically run: `devDebug`, `prodDebug`, `devRelease`, `prodRelease`, plus `devBenchmark` / `prodBenchmark` for macrobenchmarks.

## Project Structure

```text
ing-challenge/
├── app/                      Host application, navigation graph, Hilt entry point.
├── benchmark/                Macrobenchmark + Baseline Profile generator (`com.android.test`).
├── build-logic/              Included build containing convention plugins.
│   └── convention/           `recipes.android.library` and `recipes.android.feature`.
├── core/
│   ├── domain/               Pure Kotlin: models, use cases, repo interface, error mapping.
│   ├── database/             Room entities, DAO, `RecipesRepositoryImpl`, offline-first logic.
│   ├── network/              Retrofit `SpoonacularApi`, DTOs, interceptors (API key, retry, logging).
│   ├── designsystem/         Material 3 theme, `UiText`, reusable Composables (cards, chips, states).
│   ├── logging/              `Logger` interface with `TimberLogger` (debug) / `NoOpLogger` (release).
│   ├── telemetry/            `EventTracker` interface; `NoOpEventTracker` wired by default.
│   └── testing/              `FakeRecipesRepository`, fixtures for ViewModel and feature tests.
├── feature/
│   ├── search/               Search tab: ViewModel, Route/Screen, side effects.
│   ├── details/              Recipe detail with `@AssistedInject` ViewModel.
│   ├── favorites/            Favorites tab backed by the same repo Flow.
│   └── settings/             Theme / dynamic-color preferences, DataStore-backed repo.
├── config/detekt/            Detekt ruleset.
└── gradle/libs.versions.toml Shared version catalog.
```

See [`ARCHITECTURE.md`](ARCHITECTURE.md) for module dependency graph and layering.

## Testing

```bash
./gradlew test                                                    # all JVM unit tests
./gradlew jacocoCombinedReport                                    # aggregated coverage (HTML + XML)
./gradlew :app:connectedDevDebugAndroidTest                       # instrumented tests on a device
./gradlew :core:database:connectedDevDebugAndroidTest             # Room DAO instrumentation
./gradlew :benchmark:connectedBenchmarkAndroidTest                # macrobenchmarks
./gradlew :benchmark:connectedBenchmarkAndroidTest -PenableBaselineProfile=true
```

- **Unit tests** live in every module under `src/test/...` and cover domain use cases, mappers (`RecipeDtoMapper`, `RecipeEntityMapper`), interceptors, repos (Robolectric + MockK + Turbine), and all ViewModels.
- **Instrumented tests** include a `HiltGraphTest` at the `:app` level that asserts the Hilt graph resolves at runtime, and a Room DAO test in `:core:database`.
- **Coverage report** is emitted to `build/reports/jacoco/combined/html/index.html`. Hilt-generated classes, Compose previews, and activities are filtered out.
- **Macrobenchmark** measures cold startup (`StartupTimingMetric`) and list scroll (`FrameTimingMetric`) against the `search_list` test tag on `SearchScreen`.
- **Baseline Profile** is produced by `BaselineProfileGenerator` and can be consumed by the `app` module after generation.

## Quality Gates

- **Detekt** — applied to every subproject via `subprojects { apply(plugin = "io.gitlab.arturbosch.detekt") }` in the root build; config at `config/detekt/detekt.yml`.
- **KSP** — used for Hilt and Room code generation.
- **JaCoCo** — per-module unit-test coverage merged by the root `jacocoCombinedReport` task.
- **Macrobenchmark + Baseline Profile** — runtime quality gate for startup and scroll performance.
- **Android Lint** — runs as part of `assemble` / `check`.

## Architectural Summary

- **Clean architecture in three layers**: `core:domain` (pure Kotlin) → `core:network` + `core:database` (data implementations) → `feature:*` (presentation). UI never touches data directly; it always goes through a use case.
- **Offline-first**: `RecipesRepositoryImpl` writes every successful network response into Room, then emits domain `Flow`s from the DAO. The ViewModel layer observes the cache and treats the network as a refresh trigger.
- **MVVM with Route/Screen split**: each feature exposes a `Route` (Hilt + side effects + navigation) and a stateless `Screen` (testable). `StateFlow<UiState>` for rendering, `Channel<SideEffect>` for one-shot actions.
- **Material 3 + ING tokens** driven by `RecipesTheme`, with `ThemeMode` (`SYSTEM`/`LIGHT`/`DARK`) persisted in DataStore and an explicit opt-in for dynamic color.
- **Navigation 3**: typed, serializable `AppRoute` keys dispatched through `NavDisplay`, with deep links pushed onto the back stack in `LaunchedEffect`.

See [`ARCHITECTURE.md`](ARCHITECTURE.md) for full detail.

## Assumptions & Trade-offs

1. **Navigation 3 over Navigation Compose.** Chosen for typed, `@Serializable` routes and explicit back-stack ownership (`rememberNavBackStack`). Nav 3 is still early, but it removes the string-key/deep-link pattern-matching fragility of Nav Compose and matches the direction Google is steering toward.
2. **Offline-first with Room + a repository layer, not OkHttp HTTP cache.** OkHttp's disk cache is opaque to the UI, cannot be queried, and cannot express "favorited". Room gives us a queryable cache, lets us model `isFavorite` inline, and lets the UI observe changes reactively through `Flow`.
3. **Hilt over Koin.** Compile-time validation catches graph errors before they ship, integrates first-class with Compose (`hiltViewModel`, `@AssistedInject`), and composes cleanly with KSP. Koin would have been faster to set up but adds runtime cost and removes a compile-time guarantee.
4. **MVVM with Route/Screen split, no single `onEvent` dispatcher.** Named public methods on the ViewModel (`onSearchQueryChanged`, `refresh`, `loadNextPage`) are discoverable and testable without having to mirror a sealed `Event` hierarchy. The `Screen` stays stateless and accepts plain callbacks, which is more idiomatic for Compose previews and tests.
5. **Dynamic color is behind a user preference.** Material You dynamic color can violate ING's brand contract and, on some wallpapers, degrade contrast. We default to the ING tonal palette and let the user opt in (Settings → Dynamic color) on Android 12+.
6. **API key in `local.properties` injected via `BuildConfig`.** Good enough for a coding challenge because the key never touches VCS, but it is not production-safe: `BuildConfig` strings are trivially extractable from the APK. A production app would proxy requests through a backend or use Play Integrity + per-install tokens.
7. **Paging implemented manually, not with Paging 3.** The offset-based cursor in `SearchViewModel` (`offset`, `hasMorePages`, `PAGE_SIZE = 20`) is a handful of lines, composes cleanly with `StateFlow`, and avoids pulling in Paging 3's `PagingSource`/`RemoteMediator` ceremony for a feature set this small. If the product grew to multiple paginated surfaces, Paging 3 would pay for itself.
8. **No UI tests at the `:app` level (beyond Hilt graph + navigation smoke).** Each feature's stateless `Screen` is trivially testable in isolation with the Compose test rule, so duplicating those assertions through the full navigation host would give us slow tests and coverage we already have. End-to-end coverage is expressed at the macrobenchmark level against `search_list`.

## Areas for Future Improvement

- Register an `<intent-filter>` for the `recipes://recipe/<id>` scheme in `AndroidManifest.xml` so the existing `DeepLinkParser` can actually be triggered by the OS. The parser is tested, but not yet wired up as an exported deep link.
- Replace ad-hoc paging with Paging 3 if the product expands to multiple paginated lists; keep the offline-first semantics by wrapping the DAO as a `RemoteMediator`.
- Wire Coil image pre-caching for list rows to reduce first-frame jank on slow networks; currently every card fetches its thumbnail on composition.
- Replace `NoOpEventTracker` and `NoOpLogger` (release) with a real crash reporter (Crashlytics / Sentry) and a real analytics pipeline, gated by a user consent flag.
- Move the Spoonacular API key out of `BuildConfig` and behind a thin backend proxy (or at minimum obfuscate it with the NDK and attestation).
- Introduce `androidx.benchmark.BaselineProfile` plugin wiring end-to-end so the profile emitted by `BaselineProfileGenerator` is automatically consumed by `:app` during release builds.
- Add a small UI smoke test on the navigation host (search → details → back) once Navigation 3 stabilises its testing APIs.
