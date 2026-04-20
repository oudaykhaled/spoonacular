# Architecture

## Overview

ING Recipes is a multi-module, offline-first, Jetpack Compose application organised around clean architecture. The domain layer is pure Kotlin and owns the abstract model; data modules implement that model against Retrofit and Room; feature modules render it via Compose and Hilt-injected ViewModels. The `:app` module is a thin host whose only responsibility is wiring Hilt, providing the theme, and driving navigation. Cross-cutting concerns (design system, logging, telemetry, testing) live in dedicated `core:*` modules so every feature depends on abstractions, not on each other.

## Module Graph

```mermaid
flowchart LR
    app[":app"]
    search[":feature:search"]
    details[":feature:details"]
    favorites[":feature:favorites"]
    settings[":feature:settings"]
    domain[":core:domain"]
    database[":core:database"]
    network[":core:network"]
    designsystem[":core:designsystem"]
    logging[":core:logging"]
    telemetry[":core:telemetry"]
    testing[":core:testing"]
    benchmark[":benchmark"]

    app --> search
    app --> details
    app --> favorites
    app --> settings
    app --> database
    app --> network
    app --> designsystem
    app --> logging
    app --> telemetry

    search --> domain
    search --> designsystem
    search --> telemetry
    details --> domain
    details --> designsystem
    details --> telemetry
    favorites --> domain
    favorites --> designsystem
    settings --> designsystem

    database --> domain
    database --> network
    database --> logging
    network --> domain

    testing --> domain
    benchmark -. targetProjectPath .-> app
```

| Module | Role |
| --- | --- |
| `:app` | Host `Application`, `MainActivity`, Hilt entry point, navigation graph, theme wiring, app-level Hilt graph test. |
| `:benchmark` | `com.android.test` module — macrobenchmarks (cold startup, scroll) and Baseline Profile generator targeting `:app`. |
| `:core:domain` | Pure Kotlin models (`Recipe`, `RecipeDetails`, `Ingredient`, `RecipeStep`, `SortOrder`, `PageResult`, `ErrorKind`), use cases, repository interface, error mapping (`Throwable.toErrorKind()`). |
| `:core:database` | Room entities, `RecipesDao`, `RecipeEntityMapper`, and `RecipesRepositoryImpl` (the offline-first repo that implements `:core:domain`'s interface). |
| `:core:network` | Retrofit `SpoonacularApi`, DTOs, `DtoMapper`, interceptors (API key, common headers, retry) and the OkHttp client assembled via `@IntoSet InterceptorEntry` with ordered application/network phases. |
| `:core:designsystem` | Material 3 theme (`RecipesTheme`, `ThemeMode`), tonal palette, `Spacing`/`Sizing`/`Shape`/`Type` tokens, shared Composables (`RecipeCard`, `SearchBar`, `SortChipsRow`, `LoadingState`, `ErrorState`, `EmptyState`, `ConnectivityBanner`, `FavoriteButton`) and the `UiText` localisation primitive. |
| `:core:logging` | `Logger` interface with flavour-scoped implementations: `TimberLogger` in `src/debug`, `NoOpLogger` in `src/release`. |
| `:core:telemetry` | `EventTracker` interface with `NoOpEventTracker` as the default binding. |
| `:core:testing` | `FakeRecipesRepository` and `TestFixtures` used by feature unit tests so they can avoid touching Room or Retrofit. |
| `:feature:search` | Search tab — `SearchViewModel`, `SearchRoute`, `SearchScreen`, infinite scroll, debounce. |
| `:feature:details` | Recipe detail with `@AssistedInject` ViewModel keyed by `recipeId`. |
| `:feature:favorites` | Favorites tab backed by the same repository `Flow`. |
| `:feature:settings` | Theme + dynamic-color preferences. Owns its own `SettingsRepository` (DataStore). |

Convention plugins in `build-logic/convention/` (`recipes.android.library`, `recipes.android.feature`) keep AGP config, Compose, Hilt, KSP, flavour dimensions, and JVM target consistent across every module.

## Layering

```
Presentation  (Compose UI, ViewModel, UiState, SideEffect)
      │
      ▼
   Domain     (Use cases, repository interface, models, error mapping)
      │
      ▼
    Data      (Retrofit API, Room DAO, mappers, repository impl)
      │
      ▼
   Model      (Data classes, DTOs, entities)
```

- **Model**: `core/domain/src/main/java/.../model/Recipe.kt`, `RecipeDetails.kt`, `SortOrder.kt`, `PageResult.kt`, `ErrorKind.kt`, `ServerException.kt`. Plain, framework-free.
- **Domain**: use cases in `core/domain/src/main/java/.../usecase/` — `SearchRecipesUseCase`, `ObserveCachedRecipesUseCase`, `ObserveFavoritesUseCase`, `GetRecipeDetailsUseCase`, `ObserveRecipeDetailsUseCase`, `ToggleFavoriteUseCase`. Use cases expose `Flow` for reactive data and `suspend` for one-shots. Repository interface at `core/domain/src/main/java/.../repository/RecipesRepository.kt`.
- **Data**: `core/database/src/main/java/.../repository/RecipesRepositoryImpl.kt` is the single implementation of `RecipesRepository`; it owns the offline-first contract. Retrofit lives behind it; Room is the source of truth the UI observes.
- **Presentation**: `feature/*/src/main/java/.../viewmodel/*ViewModel.kt` and `feature/*/src/main/java/.../presentation/*Route.kt` + `*Screen.kt`.

**Invariant**: UI never touches `core:database` or `core:network` directly. It depends on `core:domain` only. This is enforced at the Gradle level — feature modules declare `implementation(project(":core:domain"))` but *not* `:core:database` or `:core:network`. The `:app` module is the only place those data modules are wired in to provide the concrete repository implementation.

## Packages & Conventions

Root package: `nl.ing.assessment.recipes`.

```
nl.ing.assessment.recipes
├── core
│   ├── domain         (model, usecase, repository, mapper)
│   ├── database       (entity, dao, mapper, repository, di)
│   ├── network        (dto, api, interceptor, mapper, di)
│   ├── designsystem   (theme, component, util)
│   ├── logging        (Logger, TimberLogger, NoOpLogger, di)
│   ├── telemetry      (EventTracker, NoOpEventTracker, di)
│   └── testing        (fakes, fixtures)
├── feature
│   ├── search         (viewmodel, presentation)
│   ├── details        (viewmodel, presentation.components)
│   ├── favorites      (viewmodel, presentation)
│   └── settings       (data, viewmodel, presentation, di)
├── app                (MainActivity, IngRecipesApplication, navigation, di)
└── benchmark          (RecipesBenchmark, BaselineProfileGenerator)
```

Gradle conventions:

- `build-logic/convention/src/main/kotlin/RecipesAndroidLibraryPlugin.kt` — every `core:*` and `feature:*` module applies this; it sets `compileSdk=36`, `minSdk=24`, JVM 11, `dev`/`prod` flavours, unit-test coverage, and only enables `androidTest` when the module has android test sources.
- `build-logic/convention/src/main/kotlin/RecipesAndroidFeaturePlugin.kt` — extends the library plugin with Compose, Kotlin Serialization, Hilt, and KSP. Applied by every `feature:*` module (and, because `:app` also includes Compose and Hilt, by `:app`).
- Shared versions live in `gradle/libs.versions.toml`. Never hard-code a version in a module's `build.gradle.kts`.

## Key Patterns

### ViewModel

```kotlin
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRecipes: SearchRecipesUseCase,
    private val observeCachedRecipes: ObserveCachedRecipesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val eventTracker: EventTracker,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private val _sideEffects = Channel<SearchSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    fun onSearchQueryChanged(query: String) { /* ... */ }
    fun refresh() { /* ... */ }
    fun loadNextPage() { /* ... */ }
    fun toggleFavorite(recipe: Recipe) { /* ... */ }
}
```

- `StateFlow<UiState>` for everything the UI renders.
- `Channel<SideEffect>` for one-shot actions (`NavigateToDetails`, `ShowSnackbar`, `OpenUrl`) — these must not be part of the rendered state because they are consumed exactly once.
- Named public methods, not an `onEvent(event: Event)` dispatcher. See `feature/search/src/main/java/.../viewmodel/SearchViewModel.kt`.

### Route / Screen split

- `Route` composable is the only thing the navigation graph knows about. It owns the Hilt `viewModel()`, the `collectAsStateWithLifecycle`, and the `LaunchedEffect { viewModel.sideEffects.collect { ... } }` that converts side effects into navigation calls and snackbars.
- `Screen` composable is stateless: it takes `state` plus callbacks, has no Hilt dependency, and can be rendered in a preview or Compose test without any ViewModel.

```kotlin
// feature/search/src/main/java/.../presentation/SearchRoute.kt
@Composable
fun SearchRoute(onNavigateToDetails: (Int) -> Unit, onShowSnackbar: (String) -> Unit,
                viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is SearchSideEffect.NavigateToDetails -> onNavigateToDetails(effect.recipeId)
                is SearchSideEffect.ShowSnackbar -> onShowSnackbar(effect.message.asString(context))
            }
        }
    }
    SearchScreen(state = state, onSearchQueryChanged = viewModel::onSearchQueryChanged, ...)
}
```

### Assisted injection for parameterised ViewModels

`DetailsViewModel` depends on `recipeId`, which is runtime data, so it uses `@AssistedInject` with `@HiltViewModel(assistedFactory = DetailsViewModel.Factory::class)`. See `feature/details/src/main/java/.../viewmodel/DetailsViewModel.kt`.

### `UiText`

Localisation is expressed through a sealed interface so ViewModels can return strings without depending on `Context`:

```kotlin
// core/designsystem/src/main/java/.../util/UiText.kt
sealed interface UiText {
    data class Raw(val value: String) : UiText
    data class Resource(@StringRes val resId: Int, val args: List<Any> = emptyList()) : UiText
}
```

Resolution happens inside a `@Composable` (`asString()`) or with an explicit `Context` (`asString(context)` from the `Route`'s side-effect collector).

### Immutable UI state

Every `*UiState` is annotated `@Immutable` and collections are `kotlinx.collections.immutable.ImmutableList`. This lets Compose skip recompositions reliably when the reference is unchanged.

### Repository offline-first contract

```kotlin
// core/database/src/main/java/.../repository/RecipesRepositoryImpl.kt
override suspend fun searchRecipes(query, sort, offset, number): PageResult {
    val response = wrapHttpException { api.searchRecipes(query, sort.toApiParam(), null, offset, number) }
    val entities = response.results.map { RecipeMapperHelper.summaryToEntity(it, now, json) }
    dao.upsertSearchResults(entities)                  // <-- cache first
    val favorites = dao.getFavoriteIds().toSet()       // <-- preserve isFavorite
    val items = entities.map { e -> (if (e.id in favorites) e.copy(isFavorite = true) else e).toDomain() }
    return PageResult(items, response.totalResults, response.offset, hasMore = ...)
}
```

Every successful network result is written to Room *before* it is returned. The favorite flag is always reapplied from the DAO so a remote `false` cannot silently unfavorite a recipe the user starred locally.

### Error mapping

`Throwable.toErrorKind()` in `core/domain/src/main/java/.../mapper/ErrorKindMapper.kt` classifies any thrown error as `Network`, `Server`, `RateLimited`, or `Unknown`. ViewModels then convert that `ErrorKind` into a `UiText` pointing at the design-system string resource (`error_network`, `error_server`, `error_rate_limited`, plus a feature-local fallback).

## Navigation

The navigation graph lives in `app/src/main/java/.../navigation/AppNavigation.kt` and is built on Navigation 3:

```kotlin
val backStack = rememberNavBackStack(AppRoute.Search)
NavDisplay(
    backStack = backStack,
    onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
    entryProvider = entryProvider {
        entry<AppRoute.Search>    { SearchRoute(onNavigateToDetails = { id -> backStack.add(AppRoute.Details(id)) }, ...) }
        entry<AppRoute.Favorites> { FavoritesRoute(...) }
        entry<AppRoute.Settings>  { SettingsRoute(onBack = { backStack.removeLastOrNull() }) }
        entry<AppRoute.Details>   { route -> DetailsRoute(recipeId = route.recipeId, onBack = { ... }, ...) }
    },
)
```

Routes are typed, serializable `NavKey`s:

```kotlin
// app/src/main/java/.../navigation/AppRoute.kt
@Serializable
sealed interface AppRoute : NavKey {
    @Serializable data object Search : AppRoute
    @Serializable data class Details(val recipeId: Int) : AppRoute
    @Serializable data object Favorites : AppRoute
    @Serializable data object Settings : AppRoute
}
```

**Deep links**. `DeepLinkParser` accepts `recipes://recipe/<id>` and returns the id or `null`:

```kotlin
// app/src/main/java/.../navigation/DeepLinkParser.kt
fun parseRecipeDeepLink(uri: Uri?): Int? {
    if (uri == null || uri.scheme != "recipes" || uri.host != "recipe") return null
    return uri.pathSegments.firstOrNull()?.toIntOrNull()
}
```

`MainActivity` passes the parsed id to `AppNavigation(initialDeepLinkRecipeId = ...)`, which pushes it onto the back stack in a `LaunchedEffect`, preserving the `Search` root underneath so Back returns to the list.

**Bottom bar** is rendered by `AppNavigation` only when `backStack.size <= 1`. Selecting a root tab clears the stack and adds the new root key; non-root destinations pop instead of switching tabs.

## Theming

- ING tonal palette in `core/designsystem/src/main/java/.../theme/Color.kt` (Primary40 `#FF6200`, Secondary40 `#0054A6` navy, Tertiary40 `#7A5900` warm gold, plus full neutral + error ramps).
- `RecipesTheme` assembles both `lightColorScheme` and `darkColorScheme` from those tokens. Live at `core/designsystem/src/main/java/.../theme/Theme.kt`.
- `ThemeMode` is `SYSTEM` / `LIGHT` / `DARK`; persisted in DataStore by `SettingsManager` (`app/src/main/java/.../settings/SettingsManager.kt`) under `theme_mode`.
- Dynamic color is opt-in and only applied on Android 12+ (`Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`). When enabled, `dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)` replaces the ING palette; otherwise the ING tonal palette is used.
- `Spacing`, `Sizing`, `Shape`, `Type` tokens are exposed as `CompositionLocal`s (`MaterialTheme.spacing`, `MaterialTheme.sizing`) so feature code never hard-codes a dp value.

## Data Flow

```mermaid
sequenceDiagram
    participant U as User
    participant S as SearchScreen
    participant R as SearchRoute
    participant VM as SearchViewModel
    participant UC as SearchRecipesUseCase
    participant Repo as RecipesRepositoryImpl
    participant API as SpoonacularApi
    participant DB as RecipesDao

    U->>S: types "pasta"
    S->>VM: onSearchQueryChanged("pasta")
    VM->>VM: debounce(350 ms) via searchQueryFlow
    VM->>UC: invoke("pasta", sort, offset=0, 20)
    UC->>Repo: searchRecipes(...)
    Repo->>API: GET /recipes/complexSearch
    API-->>Repo: SearchResponseDto
    Repo->>DB: upsertSearchResults(entities)
    Repo->>DB: getFavoriteIds()
    Repo-->>UC: PageResult
    UC-->>VM: PageResult
    VM->>VM: _state.update { recipes = merged, hasMorePages = ... }
    DB-->>VM: observeCachedRecipes() Flow emission
    VM-->>R: StateFlow<SearchUiState>
    R-->>S: recomposes
    S-->>U: list rendered
```

The cached-recipes `Flow` (`observeCachedRecipes`) runs independently of the search call, so changes from `toggleFavorite` or from `fetchRecipeDetails` propagate into the search list without re-issuing a network request.

## Testing Strategy

| Layer | Tooling | Examples |
| --- | --- | --- |
| Domain + mappers | JUnit, pure | `core/domain/src/test/.../SearchRecipesUseCaseTest.kt`, `ObserveFavoritesUseCaseTest.kt`, `ToggleFavoriteUseCaseTest.kt`, `ErrorKindMapperTest.kt`, `SortOrderTest.kt` |
| Repo | Robolectric + MockK + Turbine | `core/database/src/test/.../RecipesRepositoryImplTest.kt`, `RecipeEntityMapperTest.kt` |
| Network DTO + interceptors | MockWebServer + JUnit | `core/network/src/test/.../RecipeDtoMapperTest.kt`, `ApiKeyInterceptorTest.kt`, `RetryInterceptorTest.kt`, `CommonHeadersInterceptorTest.kt` |
| ViewModel | `FakeRecipesRepository` from `:core:testing`, coroutines-test, Turbine | `feature/search/src/test/.../SearchViewModelTest.kt`, `feature/details/src/test/.../DetailsViewModelTest.kt`, `FavoritesViewModelTest.kt`, `SettingsViewModelTest.kt` |
| Compose UI | `ComposeTestRule` against stateless `Screen` composables | (per feature, opt-in) |
| Room DAO | Android instrumentation | `core/database/src/androidTest/.../RecipesDaoTest.kt` |
| Hilt graph | Instrumented `HiltAndroidTest` | `app/src/androidTest/.../HiltGraphTest.kt`, `FakeTestRepositoryModule.kt`, `HiltTestRunner.kt` |
| Macrobenchmark | `MacrobenchmarkRule` | `benchmark/src/main/.../RecipesBenchmark.kt` (cold startup, scroll `search_list`) |
| Baseline profile | `BaselineProfileRule` | `benchmark/src/main/.../BaselineProfileGenerator.kt` |

`:core:testing` is explicitly excluded from the aggregated coverage report (along with `:benchmark`) so test-support code does not dilute the numbers.

## Build

- **Convention plugins**: `recipes.android.library` and `recipes.android.feature` (in `build-logic/convention`). Every library module applies the former; every feature module applies the latter.
- **Flavour dimension** `environment`: `dev`, `prod`. Both resolve to the same `BASE_URL` today but exist so alternative environments can be wired without touching Kotlin. See `core/network/build.gradle.kts`.
- **Build types**: `debug`, `release`, and a `benchmark` type declared in `:benchmark` with `matchingFallbacks += "release"` so it can run against the release variant of `:app`.
- **`BuildConfig`** fields injected from `local.properties` / env: `SPOONACULAR_API_KEY`, `BASE_URL`, `VERSION_NAME`, `DEBUG_INTERCEPTORS`.
- **Source sets per build type**: `core:network/src/debug/...` adds an `HttpLoggingInterceptor` via `DebugInterceptorModule` (Hilt `@IntoSet`); `src/release/...` adds a no-op `ReleaseInterceptorModule`. `core:logging` has the same split for `TimberLogger` vs `NoOpLogger`.
- **KSP** drives Hilt (`hilt-android-compiler`) and Room (`androidx.room`) code generation.
- **Detekt** is wired in the root `build.gradle.kts` via `subprojects { apply(plugin = "io.gitlab.arturbosch.detekt") }` with `config/detekt/detekt.yml`.
- **JaCoCo aggregated report**: the root `jacocoCombinedReport` task (`build.gradle.kts`) depends on every module's `testDevDebugUnitTest`, collects `.exec` files from `outputs/unit_test_code_coverage/devDebugUnitTest`, and emits HTML + XML at `build/reports/jacoco/combined/`. Generated Hilt classes, activities, Compose previews, serializers, and Room `_Impl` classes are filtered out via `fileFilter`.
- **`allInstrumentedTests`** root task discovers every module that actually has `src/androidTest` sources and runs their `connectedDevDebugAndroidTest` tasks.

## Known Limitations

- The `recipes://recipe/<id>` deep link is parsed and unit-tested, but no `<intent-filter>` is registered in `AndroidManifest.xml`, so the OS cannot currently route a URL to the app. Adding this is a one-line manifest change, but it has not been wired.
- Paging is offset-based and manual. There is no `RemoteMediator`, no transparent retry on cursor loss, and no cross-session restoration of the paging state. Paging 3 would replace this if the feature set grew.
- Image loading (Coil) has no explicit pre-cache strategy; list rows trigger a fetch on composition. Scroll jank is not profiled beyond the general macrobenchmark.
- Crash reporting is a `NoOpLogger` / `NoOpEventTracker` in release. A production build would need real implementations wired behind a consent flag.
- `SPOONACULAR_API_KEY` is injected into `BuildConfig`, which puts the string literal in the APK. This is acceptable for a challenge but is not a secrets boundary.
- The benchmark `buildType` exists in `:benchmark` with `matchingFallbacks = "release"`, but `:app` does not declare its own `benchmark` buildType — consumers rely on the fallback. Declaring one explicitly would make the variant matrix more self-documenting.
- There is no CI pipeline configured in the repo; the quality gates (unit, Detekt, JaCoCo, instrumentation, macrobenchmark) all exist as Gradle tasks but must be invoked manually.
