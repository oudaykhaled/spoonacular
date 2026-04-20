# ING Recipes — Deep Assessment Report

> **Reviewer:** Staff/Principal Android Engineer review (cold read of source code only — source is the sole source of truth).
> **Target:** `ing-challenge/` against the ING assessment brief + internal `android-engineering` skill set (architecture / compose / design-system / flow-coroutines / testing / benchmark).
> **Scope:** 133 Kotlin files across 14 Gradle modules, 21 unit-test files, 5 instrumentation files, 1 macrobenchmark module. No runtime verification — findings are from static inspection only.
> **Severity scale:** `Critical` = ships-broken or leaks data · `High` = breaks a stated requirement or skill invariant · `Medium` = wrong for production but functional · `Low` = cosmetic / nit.

---

## 0. TL;DR

The project is **well-architected on paper** — clean multi-module graph, convention plugins, Hilt with `@IntoSet` interceptors, `@AssistedInject` details VM, offline-first repo with `@Transaction` upserts, Navigation 3, MVVM route/screen split, UiText, Material 3 tonal tokens, and a benchmark module. The `README.md` and `ARCHITECTURE.md` are excellent.

**But several things claimed in the docs are not true in the source, and several will break at runtime.** In order of severity:

1. **Critical — Theme settings don't actually work.** `:app/settings/SettingsManager` and `:feature:settings/SettingsRepositoryImpl` are **two independent `preferencesDataStore` files** (`app_settings` vs `settings`). `MainActivity` reads from the former; `SettingsViewModel` writes to the latter. Changing theme / dynamic-color in the UI has **zero runtime effect**.
2. **Critical — Hilt instrumentation tests cannot work.** `HiltTestRunner` exists but `testInstrumentationRunner` is hard-coded to `androidx.test.runner.AndroidJUnitRunner` in `:app`, the convention plugin, and `:benchmark`. `HiltTestApplication` is never loaded, so `@HiltAndroidTest` (`HiltGraphTest`, `AppNavigationFlowTest`) will fail on a device.
3. **Critical — Benchmark cannot find `search_list`.** `testTagsAsResourceId = true` is not set anywhere in the app tree, so `By.res("search_list")` in `RecipesBenchmark` and `BaselineProfileGenerator` resolves nothing. The scroll benchmark and the baseline profile journey are effectively no-ops.
4. **High — Release build is not minified.** `:app/build.gradle.kts` has `isMinifyEnabled = false` for `release`, contradicting `README.md` ("proguard-ready") and the skill's "Release builds: `minifyEnabled = true`, `shrinkResources = true`". No baseline profile wiring, no `profileinstaller` on `:app`.
5. **High — Timber `DebugTree` planted in release.** `IngRecipesApplication.onCreate()` calls `Timber.plant(Timber.DebugTree())` unconditionally, **bypassing** the debug/release source-set split that `core:logging` carefully set up. `TimberLogger` also self-plants in `init{}`.

Everything else below is recoverable with small, targeted changes. The foundation is strong.

---

## 1. Requirements checklist (from the assessment brief)

| # | Requirement | Status | Notes |
|---|---|---|---|
| 1 | Search bar at top, searches by name/ingredient | [x] | Debounced 350 ms in `SearchViewModel.setupSearchDebounce()`. |
| 2 | `spoonacular.com/recipes/complexSearch` | [x] | `SpoonacularApi.searchRecipes` with `addRecipeInformation=true`. |
| 3 | Scrollable list of recipe cards (name, image, description) | [x] | `RecipeCard` + `LazyColumn` + `AsyncImage` + stripped HTML summary. |
| 3-bonus | Sorting options | [x] | `SortOrder {RELEVANCE, POPULARITY, HEALTHINESS, TIME, PRICE}` → `SortChipsRow`. |
| 4 | Recipe detail screen (name, image, ingredients, instructions, link) | [x] | `DetailsScreen` + `DetailsHeader / IngredientsSection / InstructionsSection / SourceButton`. |
| 5 | Local DB caches viewed recipes / offline | [x] | Room + `@Transaction upsertRecipeDetails`; repo writes before returning. |
| 5-bonus | Mark favorites + view offline | [x] | `setFavorite` DAO, `observeFavorites`, `FavoritesScreen`. Preservation verified in DAO test. |
| 6 | MVVM, Model/Domain/Data/UI separation | [x] | Clean module boundaries; `:core:domain` owns the `RecipesRepository` contract. |
| 7 | Dark + light theme from system | [x] | `ThemeMode.SYSTEM` + `isSystemInDarkTheme()`. |
| 7-opt | In-app toggle light/dark | [~] | **UI exists, but toggle is a no-op at runtime (see Critical #1).** |
| 7-bonus | Material You dynamic color | [~] | **Same — settings switch is wired to an orphan DataStore.** Logic in `Theme.kt` is correct. |
| 7-bonus 2 | ING brand theme | [x] | Primary40 `#FF6200`, Secondary40 `#0054A6`, Tertiary40 `#7A5900`. |
| 8 | Well-organized, documented codebase | [x] | Strong `README.md` and `ARCHITECTURE.md`; module naming is consistent. |
| 9 | Graceful error handling | [x] | `ErrorKind` + `Throwable.toUiText()` + `ErrorState`. `dismissError` wired. |
| 10 | Unit tests for Domain + Data | [x] | 12 unit-test files across domain/data/network. DAO test included. |
| 11 | UI tests for views | [ ] | **Only `AppNavigationFlowTest` exists and it only asserts `waitForIdle()`** — no per-screen Compose test files. `README.md` lists them as "(per feature, opt-in)". |
| extras | Deep link | [x] | Parser + tests + manifest `intent-filter` (`recipes://recipe/<id>`). |
| extras | Macrobenchmark + baseline profile | [~] | Code present, but **see Critical #3 (cannot find list node)**. |

---

## 2. Architecture & build (skill: `architecture.md`)

### What's done right

- [x] Multi-module graph matches `ARCHITECTURE.md`. No feature→feature imports.
- [x] `RecipesRepository` lives in `:core:domain`; feature modules do not depend on `:core:database` / `:core:network`.
- [x] Convention plugins `recipes.android.library` / `recipes.android.feature` exist and are applied.
- [x] Hilt `@InstallIn(SingletonComponent::class)` everywhere; `@Binds` abstract vs `@Provides` object split is correct.
- [x] `@IntoSet InterceptorEntry` with `order` + `type`.
- [x] Debug/Release source-set split for interceptors (`DebugInterceptorModule` / empty `ReleaseInterceptorModule`) and for logger (`TimberLogger` / `NoOpLogger`).
- [x] `@AssistedInject` + `@HiltViewModel(assistedFactory = DetailsViewModel.Factory::class)` for `recipeId`.
- [x] `@TestInstallIn(replaces = [RepositoryModule::class])` test double module exists.
- [x] Navigation 3 routes are `@Serializable sealed interface AppRoute : NavKey`.
- [x] Deep link parsed via `DeepLinkParser` (6 unit tests cover every branch) and wired to `AppNavigation(initialDeepLinkRecipeId)`.
- [x] Root `build.gradle.kts` has `allInstrumentedTests`, `jacocoCombinedReport`, Detekt applied to every subproject.
- [x] Room schema exported to `core/database/schemas/`.

### What's wrong / missing

- [ ] **[Critical] Two parallel Settings DataStores.** `:app` has `SettingsManager` → `preferencesDataStore("app_settings")` used by `MainActivity`; `:feature:settings` has `SettingsRepositoryImpl` → `preferencesDataStore("settings")` used by `SettingsViewModel`. The feature writes; the app never reads from that store. The user's theme selection is silently dropped. — `app/src/main/java/nl/ing/assessment/recipes/settings/SettingsManager.kt` vs `feature/settings/.../data/SettingsRepositoryImpl.kt`.
- [ ] **[Critical] Hilt test runner not wired.** `:app/build.gradle.kts:20`, `:benchmark/build.gradle.kts:11`, and the convention plugin all set `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`. `HiltTestRunner` class exists but is never referenced. Every `@HiltAndroidTest` (`HiltGraphTest`, `AppNavigationFlowTest`) will fail at `hiltRule.inject()` because `HiltTestApplication` is not installed.
- [ ] **[High] Release is not minified.** `:app/build.gradle.kts:32` — `isMinifyEnabled = false`. No `shrinkResources`. `README.md` explicitly says "release (proguard-ready)" and the skill says `minifyEnabled = true`.
- [ ] **[High] No `benchmark` build type on `:app`.** `:benchmark` declares one with `matchingFallbacks += "release"`, so it silently runs against the non-minified `release` variant, defeating the purpose of macrobenchmarks. The skill explicitly requires a `benchmark` build type on `:app`.
- [ ] **[High] No `profileinstaller` dependency on `:app`.** `libs.androidx.profileinstaller` is declared in the catalog but never consumed. The baseline profile, even if it were captured successfully, would not be installed.
- [ ] **[High] No `androidx.baselineprofile` plugin wiring.** Profile generation task exists, but nothing consumes the emitted `baseline-prof.txt` into `:app/src/main/`. Confirmed as known gap in `ARCHITECTURE.md § Known Limitations`, but still rated High against the skill which requires "commit the updated `baseline-prof.txt`".
- [ ] **[Medium] Convention plugin duplicates feature plugins.** `:app/build.gradle.kts` re-applies `com.android.application`, Compose, Hilt, KSP manually instead of using a `recipes.android.application` convention plugin. `core/database/build.gradle.kts` re-applies `kotlin-serialization`, `hilt-android`, `ksp` that are already applied by `recipes.android.library`'s feature sibling. Skill expects "Convention plugins are mandatory for all library/feature modules".
- [ ] **[Medium] `dev`/`prod` flavors are identical.** Both resolve to `https://api.spoonacular.com/`. The flavor dimension exists but has no functional purpose today. Not a bug, but a documentation-vs-reality mismatch (readme implies separation).
- [ ] **[Medium] `:feature:settings` is missing from `ARCHITECTURE.md`'s mermaid graph dependencies** (`settings --> designsystem` is shown, but `:app` depending on `:feature:settings` is shown and `SettingsRepository` needs `:core:designsystem` for `ThemeMode`, which is in design system — unusual location). Minor coupling smell: `ThemeMode` is domain-level configuration but lives in design system.
- [ ] **[Medium] `IngRecipesApplication` uses Timber.plant directly.** Skill §2 is explicit: "Debug-only behavior (logging) lives in `src/debug/` source sets… never `if (BuildConfig.DEBUG)` guards in shared code." The Application goes further — not even a guard, unconditional plant — defeating the source-set split that `core:logging` already performs. Release builds ship `DebugTree`.
- [ ] **[Low] `AppModule` only provides a clock lambda.** It's never injected anywhere (`RecipesRepositoryImpl` uses a default parameter fallback to `System.currentTimeMillis()`). Dead wiring.
- [ ] **[Low] `FakeTestRepositoryModule` uses `@Provides` instead of `@Binds`.** Works, but the skill example uses `@Binds abstract`. It also instantiates a new fake inline, so tests can't reach into the instance to flip `failSearch` etc.
- [ ] **[Low] `:app` depends on `:core:database` and `:core:network`.** The skill module graph lists `:app → :core:database / :core:network` only to wire DI; this is consistent. Noted, not a problem, just confirming.

---

## 3. Jetpack Compose (skill: `compose.md`)

### What's done right

- [x] `*Route` + `*Screen` split across Search, Favorites, Details, Settings.
- [x] `hiltViewModel()` in Routes; Screens are stateless.
- [x] `collectAsStateWithLifecycle()` used everywhere (no `collectAsState()`).
- [x] `rememberUpdatedState` on navigation/snackbar callbacks in Routes.
- [x] `Channel(BUFFERED).receiveAsFlow()` for side effects; no `SharedFlow` or `LiveData` anywhere.
- [x] One named lambda per action on every screen — no `onEvent` dispatcher (MVVM, not MVI).
- [x] `@Immutable` on all `UiState` classes.
- [x] `ImmutableList<T>` (`kotlinx.collections.immutable`) for all list fields.
- [x] `LazyColumn` `key` + `contentType` on search and favorites lists.
- [x] Infinite scroll via `snapshotFlow { layoutInfo } .distinctUntilChanged() .filter { } .collect { onLoadNextPage() }`.
- [x] `@Stable` on `SortOrder` and `ThemeMode`.
- [x] Scoped sub-composables in feature (`DetailsHeader`, `IngredientsSection`, `InstructionsSection`, `SourceButton`) — nothing exceeds the ~60-line rule.

### What's wrong / missing

- [ ] **[Critical] `testTagsAsResourceId = true` is never set.** The skill requires it in the root `Box` of `MainActivity` for UIAutomator / benchmark selectors. Without it, `By.res("search_list")` and `By.res("bottom_navigation_bar")` resolve nothing. `RecipesBenchmark.scrollRecipesList()` and `BaselineProfileGenerator.generate()` are silently useless (`findObject` returns null → `?.fling` no-ops).
- [ ] **[High] `SearchScreen` has no pull-to-refresh UI.** `SearchViewModel.refresh()` exists and `onRefresh` is plumbed through `SearchRoute → SearchScreen → SearchContent → ErrorState`, but it's **only** invoked from the error-state retry button. `README.md` claims "Infinite scroll and pull-to-refresh", and the ViewModel tracks `isRefreshing`, but no `PullToRefreshBox` / `LottiePullToRefreshIndicator` is present in any screen. The state field is dead.
- [ ] **[High] `FavoritesScreen.onDismissError` is accepted but never called.** `FavoritesContent` never renders an error banner or dismiss action. The VM can set `error` but the UI has no path to clear it except via the next success emission.
- [ ] **[High] `ConnectivityBanner` inside a `Column` above the Scaffold'd screens.** Only `SearchScreen` includes it. `FavoritesScreen` and `DetailsScreen` do not show connectivity status. Moreover, `AppNavigation` places `SnackbarHost` raw in a `Column` — not `Scaffold.snackbarHost` — so it will not animate/insets correctly and will stack above the search content instead of floating.
- [ ] **[Medium] No `@Preview`s.** `SettingsScreen` has a private `SettingsScreenPreview` — but it's **not** annotated with `@Preview`, so Android Studio will not render it. The skill's Previews policy says "Never check in a screen without at least one preview for its stateless `*Screen` composable" (aspirational, marked as not-yet-enforced, but called out).
- [ ] **[Medium] `FavoriteButton` content-description double-announcement guarded, but `RecipeCard` is not.** `RecipeCard` has no `modifier.semantics { contentDescription = … }` at the card level. A screen reader will just announce the title text, missing "Favorite, 45 minutes, health 80".
- [ ] **[Medium] `SearchContent` `onRetry` shorthand.** `ErrorState { onDismissError(); onRetry() }` — the dismiss-then-retry dance is done once here but not abstracted, and `DetailsBody` repeats the same pattern. Low-risk duplication.
- [ ] **[Medium] `RecipeCard` uses `stripHtml` regex at compose time.** `remember(recipe.summary) { … stripHtml() }` is correct, but the regex matcher compiles a shared pattern (`HtmlTagRegex` is a top-level `val`, good). However, the Spoonacular `summary` contains entities like `&amp;` and `&lt;` which are not unescaped. A properly stripped summary would need `HtmlCompat.fromHtml(...).toString()` for a11y correctness.
- [ ] **[Medium] `SnackbarHost` rendered above `NavDisplay` in `Column`.** Should be a `Scaffold(snackbarHost = ...)` wrapper at the `AppNavigation` level so snackbars overlay content and respect IME/system bars. Current layout pushes the screen content down by the snackbar height.
- [ ] **[Medium] Bottom `NavigationBar` conditionally rendered on `backStack.size <= 1`.** This works for the current 3-tab / 1-detail flow, but the `currentRoot = backStack.firstOrNull()` rule means any nested navigation under a non-Search tab shows the wrong selection when popped. Fragile for future destinations.
- [ ] **[Low] `SearchScreen.PREFETCH_THRESHOLD = 4` duplicated.** Also defined as `private companion object` in `SearchViewModel`. The UI constant is separate, which is fine, but it will drift.
- [ ] **[Low] `FavoritesList` accepts `kotlinx.collections.immutable.ImmutableList<Recipe>` with fully-qualified name** instead of importing. Nit.
- [ ] **[Low] `InfoPill` in `RecipeCard` is a private composable with unused `modifier` parameter default.** Standard shape, fine.
- [ ] **[Low] `Modifier` is not the last parameter** in `InfoPill` (icon/text/colors/modifier at position 5). Skill explicitly: "`Modifier` is always the last parameter with `Modifier = Modifier` default". It is last here, confirmed OK. `FavoritesList` same. Withdrawing.

---

## 4. Design system & theming (skill: `design-system.md`)

### What's done right

- [x] Full tonal palette generated for Primary/Secondary/Tertiary/Neutral/NeutralVariant/Error in `Color.kt`. No inline `Color(0xFF…)` found in feature code (grep).
- [x] `lightColorScheme` / `darkColorScheme` fully populated with role mappings.
- [x] `Spacing` data class + `LocalSpacing` + `MaterialTheme.spacing` extension.
- [x] `Sizing` data class + `LocalSizing` + `MaterialTheme.sizing` extension (adds `recipeImageHeight`, `elevationSmall` — sensible extensions).
- [x] `RecipesTheme(themeMode, dynamicColor, content)` — `ThemeMode` enum + opt-in dynamic color gated on `Build.VERSION_CODES.S`.
- [x] `UiText` sealed interface with `Raw` / `Resource`, `@Composable asString()` and `Context asString(context)` helpers.
- [x] Module-local `strings.xml` for `:app`, `:core:designsystem`, and each `:feature:*`.
- [x] `ErrorKind` → `UiText.Resource` mapper exists (`feature/details/.../ErrorUiMapper.kt`).

### What's wrong / missing

- [ ] **[High] `Throwable.toUiText()` duplicated in three feature modules.** `SearchViewModel`, `FavoritesViewModel`, and `feature/details/…/ErrorUiMapper.kt` each carry near-identical `private/internal fun Throwable.toUiText()` implementations, each importing `DesignSystemR` separately. The skill puts this in `ErrorKindMapper.kt` next to `toErrorKind()`. Three sources of truth for error → string mapping.
- [ ] **[High] `SearchViewModel.toUiText` falls back to `R.string.error_loading` (feature-local) for `ErrorKind.Unknown`, but `FavoritesViewModel.toUiText` falls back to `DesignSystemR.string.error_unknown`.** Inconsistent UX for the same condition. The details mapper uses `error_unknown` too. Search's divergence breaks localization consistency.
- [ ] **[Medium] `ThemeMode` lives in `:core:designsystem`.** It's an enum representing user configuration, consumed by `:feature:settings` and `:app`. A design-system module owning user-preference state is an inverted dependency. It should live in `:core:domain` or `:feature:settings`.
- [ ] **[Medium] No `LightDarkPreview` / multi-preview annotation.** The skill mentions it. No `@PreviewParameter`. One orphan preview function in settings (not even annotated).
- [ ] **[Medium] No `Shape.kt` content reviewed but typography exists.** Didn't deep-read; sizes look standard.
- [ ] **[Low] `Spacing` does not expose `xxxxLarge` / `huge` is 64.dp which is used exactly once** (end-of-scroll filler). Minor.
- [ ] **[Low] No typed `Dp` wrappers** on top of `MaterialTheme.sizing.recipeImageHeight` — consumed once. Fine.
- [ ] **[Low] Bottom nav uses `contentDescription = stringResource(tab.labelRes)` while the `Text(label)` underneath carries the same text.** Double-announcement for TalkBack. Should be `contentDescription = null` or the label should be `Modifier.semantics { isTraversalGroup = true }`.

---

## 5. Kotlin Flow & coroutines (skill: `flow-coroutines.md`)

### What's done right

- [x] `MutableStateFlow(UiState()) + asStateFlow()` pattern in all four VMs.
- [x] `Channel(BUFFERED).receiveAsFlow()` for side effects in all VMs; no `SharedFlow`.
- [x] Named public methods per action (`onSearchQueryChanged`, `refresh`, `loadNextPage`, `toggleFavorite`, `onRecipeClicked`, `onOpenSource`, `onSortChanged`, `dismissError`, `retry`) — no `onEvent(event)` dispatcher.
- [x] Search debounce: `searchQueryFlow.debounce(350L).distinctUntilChanged().onEach { performSearch(…) }.launchIn(viewModelScope)`.
- [x] `_state.update { it.copy(...) }` used consistently — no `.value = .value.copy(...)` antipatterns.
- [x] Observation pipelines use `.distinctUntilChanged().onEach {}.catch {}.launchIn(viewModelScope)`.
- [x] Room `Flow` queries are the single source of truth (`observeAllRecipes`, `observeFavorites`, `observeRecipeWithRelations`).
- [x] `@Transaction upsertSearchResults` preserves existing `isFavorite` rows (verified by DAO test).
- [x] No `withContext(Dispatchers.IO)` / `Main` anywhere in app code (only one `Thread.sleep` in `RetryInterceptor`, which is correct for the OkHttp threading model).
- [x] `@OptIn(FlowPreview::class)` in `SearchViewModel` for `debounce`.
- [x] `try/catch(e: Exception)` for suspend calls inside `viewModelScope.launch { }` — matches the error-handling hierarchy.

### What's wrong / missing

- [ ] **[High] `SearchViewModel.performSearch()` is 53 lines.** The skill's §7 says non-composable functions > 30 lines must be extracted. This function mixes blank-query short-circuit, offset bookkeeping, loading flag juggling, merging with dedup, totalResults/hasMore reconciliation, error mapping. Extract `handleBlankQuery()`, `applySuccess()`, `applyError()`.
- [ ] **[High] `performSearch` catches `Exception`, not a targeted hierarchy.** The skill prefers "targeted `catch(e: Exception)` block". Here it catches everything including `CancellationException` in older Kotlin versions and potentially `OutOfMemoryError`-derived runtime exceptions. Should at least let `CancellationException` propagate or use `coroutineContext.ensureActive()` first.
- [ ] **[High] `SearchViewModel.observeCache()` silently cancels reset when searchQuery is non-blank.** Specifically, its `.catch {}` sets the error, but the "merge favorite flags" branch filters out any new cached recipe that wasn't already in the current UI list. That means when the DB adds a freshly cached recipe from network sync, the search list never sees it until the next search pass. Subtle offline-first leak.
- [ ] **[High] `SearchViewModel.toggleFavoriteInternal` mutates `favoriteLoadingIds` twice.** The `finally { _state.update { … .filter { it != id }.toImmutableList() } }` is correct, but the `try { }` doesn't sync the `isFavorite` flag optimistically in `state.recipes`. Room `Flow` will re-emit and `mergeFavoriteFlags` will pick it up, but there's a perceptible lag. Minor UX hit.
- [ ] **[High] `RetryInterceptor.Thread.sleep` is not interruptible by `InterruptedException`.** The catch for `InterruptedException` restores interrupt flag but does not bail — it continues to retry. If OkHttp's dispatcher cancels the call (coroutine cancellation), sleep is uninterruptible and cancellation is swallowed.
- [ ] **[Medium] `RetryInterceptor.backoffMillis(attempt) = 500 * (1 shl attempt)` → 500, 1000** — only 2 retries means max wait ~1.5s. Fine, but no jitter → thundering herd on recovery. Not critical for a client app.
- [ ] **[Medium] `DetailsViewModel.observeCachedDetails()` only updates state when `cached != null`** (`current.copy(details = cached ?: current.details)`). If the entity is deleted from Room, the UI will never reflect it — stale forever. Should either refetch or show an empty state.
- [ ] **[Medium] `SearchViewModel.setupSearchDebounce()` triggers `performSearch(query, reset = true)` even when `query == ""`** (first emission, before user types). Then `performSearch` short-circuits on blank, which is fine, but it means every first keystroke after clearing resets `offset`/`hasMorePages`/`totalResults` unconditionally. This masks a subtle regression: if you search "pasta", then clear the field, then re-type "pasta", the infinite-scroll state resets (expected) — OK on reflection.
- [ ] **[Medium] `FavoritesViewModel` duplicates dependencies.** Constructor-injected params are re-assigned to private properties inside the class (`observeFavoritesUseCase = observeFavorites`). Unnecessary; inject with `private val` constructor properties directly.
- [ ] **[Medium] No `initialValue` on `observeCachedRecipes` / `observeFavorites`.** State starts empty; `isLoading` defaults to `false` in `FavoritesUiState`, so the initial render shows the `EmptyState` momentarily before the first emission arrives. Minor flicker.
- [ ] **[Low] No `stateIn` / no `SharingStarted.WhileSubscribed`.** Consistent with skill ("`stateIn` / `shareIn` — use `MutableStateFlow` pattern above instead"). OK.
- [ ] **[Low] `SearchViewModel.onSearchQueryChanged` updates `searchInput` and `searchQueryFlow.value` immediately on every keystroke.** Skill says "`searchInput` in `UiState` reflects the text field value immediately (no debounce)", which matches. Confirming OK.

---

## 6. Error handling & data layer

### What's done right

- [x] `wrapHttpException { }` in `RecipesRepositoryImpl` maps Retrofit `HttpException` → `ServerException(code, message)`.
- [x] `ErrorKind.RateLimited` branch for 402/429 — Spoonacular-specific (quota) mapping.
- [x] Repo writes to Room **before** returning, then overlays favorite flag from `getFavoriteIds()` (prevents remote-false from unfavoriting).
- [x] `observeCachedRecipes()` is the UI's read path; `searchRecipes` is a refresh trigger that mutates Room; observation re-emits.
- [x] `upsertRecipeDetails` preserves `isFavorite` from existing row.
- [x] `fallbackToDestructiveMigration(dropAllTables = true)` — acceptable for a challenge (cache, not user data), with `@ColumnInfo(defaultValue = …)` defaults in place.

### What's wrong / missing

- [ ] **[High] No connectivity-aware cache fallback.** When offline, `searchRecipes()` throws `IOException` and the VM surfaces `error_network`. But `observeCachedRecipes()` still emits whatever's cached, and the UI falls into the empty/error branch before `state.recipes.isEmpty()` flips. Users see an error even though we have cached data. Offline-first UX demands "show stale + show banner", not "show error".
- [ ] **[High] `sourceUrl` is not validated before `Uri.parse()`.** In `DetailsRoute`, `Intent(Intent.ACTION_VIEW, Uri.parse(effect.url)).addFlags(…)` will crash if the URL has a malformed scheme. Spoonacular usually returns valid URLs but not always. Should validate or `try/catch ActivityNotFoundException` / `Uri.parse` null check.
- [ ] **[Medium] `dao.upsertRecipeDetails` has a race** with `setFavorite`. If the user favorites a recipe while `fetchRecipeDetails` is in flight, the recipe copy fetched from API will overwrite the favorite flag that was toggled mid-flight (because the `getRecipe` read is before the `upsertRecipes(listOf(merged))` write inside the `@Transaction`). Moderate; `@Transaction` only guarantees atomicity inside the method, not across methods.
- [ ] **[Medium] `RecipesRepositoryImpl.searchRecipes` hardcodes a 200 status log.** It logs `statusCode = 200` even though the actual response may not have been 200 (if OkHttp upgraded). Cosmetic logging issue.
- [ ] **[Medium] No TTL on cached recipes.** `cachedAt` is written but never consulted — stale data lives forever until overwritten.
- [ ] **[Low] `RecipeMapperHelper.summaryToEntity` not inspected** but referenced. Likely fine given mapper tests exist.
- [ ] **[Low] `DatabaseModule.provideRecipesDao` is not `@Singleton`.** Room DAOs are stateless and Room itself caches them — this is fine, but inconsistent with the rest of the module.

---

## 7. Testing (skill: `testing.md`)

### What's done right

- [x] MockK + Turbine + `runTest` + `StandardTestDispatcher` + `Dispatchers.setMain` pattern followed in every VM test.
- [x] `FakeRecipesRepository` in `:core:testing` with failure flags (`failSearch`, `failDetails`, `failFavorite`) and emit helpers.
- [x] Real use cases wired with the fake — `SearchRecipesUseCase(repository)` etc. — not mocked.
- [x] Repository layer tested with MockK against DAO + API.
- [x] Room DAO tests use `Room.inMemoryDatabaseBuilder` + `allowMainThreadQueries()` + Turbine.
- [x] Test naming uses backticked sentences in BDD style.
- [x] 21 unit-test files across the project — good spread.
- [x] `DeepLinkParsingTest` (Robolectric) — 6 cases covering every branch.
- [x] Interceptor tests (`ApiKeyInterceptor`, `RetryInterceptor`, `CommonHeadersInterceptor`) — `MockWebServer` + OkHttp.

### What's wrong / missing

- [ ] **[Critical] No real compose UI tests.** `ARCHITECTURE.md` table lists "Compose UI — `ComposeTestRule` against stateless `Screen` composables — (per feature, opt-in)" — but every feature has **zero** `*ScreenTest.kt` in `src/androidTest`. The brief explicitly requires "UI tests for the views". The only Compose test is `AppNavigationFlowTest` which just launches the activity and calls `waitForIdle()`.
- [ ] **[Critical] Hilt instrumentation tests will not run.** See §2 — `testInstrumentationRunner` not set to `HiltTestRunner`. Both `HiltGraphTest` and `AppNavigationFlowTest` will fail to initialize `HiltTestApplication`.
- [ ] **[High] `AppNavigationFlowTest` is a smoke test masquerading as a flow test.** It literally asserts nothing beyond activity launch. The skill's example does `onNodeWithTag(...).performClick() … assertIsDisplayed()`. Renaming would reduce misrepresentation.
- [ ] **[High] No ViewModel test for cache merge / favorite preservation edge cases.** `mergeFavoriteFlags` has a non-trivial branch (empty existing → return existing) and is not directly tested.
- [ ] **[High] `SearchViewModelTest.trackScreenView is called on init` is executed inside `runTest`, but `verify` must come after `advanceUntilIdle()`** if the dispatcher hasn't run yet — here it happens to work because `trackScreenView` is on a mock that relaxes to no-op synchronously, but flaky under real dispatch.
- [ ] **[High] `SettingsViewModelTest` uses `coEvery { themeMode } returns themeModeFlow`**, but `SettingsRepository.themeMode` is a non-suspend `val: Flow<ThemeMode>`. `coEvery` on a property getter works in MockK but is semantically odd — `every` would be the direct match.
- [ ] **[Medium] `FakeTestRepositoryModule` creates a new `FakeRecipesRepository()` inline.** Test code cannot inject seed data. Should be bound to a singleton held by the test.
- [ ] **[Medium] No test for the `RetryInterceptor` `IOException` path with interrupt.** Skill-level thoroughness missing.
- [ ] **[Medium] `DetailsViewModelTest.toggleFavorite calls repo and flips favorite state`** runs the coroutine then calls `repository.observeFavorites().first()` — which will hang if no favorite emission ever arrives. Works today because `FakeRecipesRepository` seeds `favoritesFlow` synchronously on toggle.
- [ ] **[Medium] No JaCoCo coverage gate.** The `jacocoCombinedReport` task exists, but no `minimumCoverage` rule in the build. Coverage reports run, nothing blocks on a threshold.
- [ ] **[Low] No Kotest, no property-based testing** — fine, the skill explicitly bans both.
- [ ] **[Low] Test fixtures in `TestFixtures.kt` not reviewed** but referenced widely.

---

## 8. Benchmark & baseline profile (skill: `benchmark.md`)

### What's done right

- [x] `:benchmark` module exists with `com.android.test`.
- [x] `targetProjectPath = ":app"` + `self-instrumenting = true`.
- [x] `variant.enable = variant.buildType == "benchmark"` — only the benchmark variant builds.
- [x] Flavor dimensions mirror `:app`.
- [x] `benchmark` build type: `isDebuggable = true`, `signingConfig = debug`, `matchingFallbacks += "release"`.
- [x] `suppressErrors = "EMULATOR,DEBUGGABLE"` — annotated in doc, acceptable for CI.
- [x] `RecipesBenchmark.coldStartup()` — `StartupMode.COLD`, 5 iterations, `pressHome()` setup.
- [x] `RecipesBenchmark.scrollRecipesList()` — `FrameTimingMetric`, `StartupMode.WARM`.
- [x] `BaselineProfileGenerator.generate()` — `BaselineProfileRule`.

### What's wrong / missing

- [ ] **[Critical] `By.res("search_list")` / `By.res("bottom_navigation_bar")` do not resolve** because `testTagsAsResourceId = true` is never enabled on the Compose root. Benchmark scroll silently flings nothing. The baseline profile captures only cold start + a no-op `fling`.
- [ ] **[Critical] `:app` does not declare `benchmark` build type.** `matchingFallbacks += "release"` in `:benchmark` falls back to the non-minified `release` variant (since `isMinifyEnabled = false`). Benchmarks therefore run on a non-R8 build, which is not representative of shipping performance.
- [ ] **[High] No `androidx.profileinstaller` implementation dependency on `:app`.** Baseline profile, even if captured, is not consumed.
- [ ] **[High] No `androidx.baselineprofile` plugin applied** to `:app` or `:benchmark`. AGP will not wire the generated profile into the shipping artifact.
- [ ] **[High] Baseline profile journey is minimal.** Only `startActivityAndWait()` + `fling` — no search, no navigate-to-details, no favorites, no settings. Hot paths for detail and favorites will not be covered even when the wiring is fixed.
- [ ] **[Medium] `RecipesBenchmark.scrollRecipesList`'s `setupBlock = { startActivityAndWait() }`** does not wait for search results. Without a pre-seeded search, `search_list` is empty; `fling` has no effect even if `testTag` resolution worked.
- [ ] **[Medium] 5 iterations is the skill minimum**, but no baseline profile CI wiring to track regressions.
- [ ] **[Low] No CPU/memory/trace metrics added** beyond startup/frame timing. Skill mentions this as aspirational.

---

## 9. Security / secrets

- [ ] **[Critical for prod, Medium for a coding challenge] Spoonacular API key is embedded in `BuildConfig`.** Any APK ships the string. `README.md` acknowledges this. For a challenge, it's standard; for production, it needs a backend proxy or per-install token.
- [x] `local.properties` is gitignored (`.gitignore:3,10`).
- [ ] **[High] A real Spoonacular API key `0af19e74f0264d17bffae78bf042e92b` is in `ing-challenge/local.properties`.** Though the file is gitignored, this key was shared to the reviewer by virtue of the file being present on disk. If this key was ever committed in a prior branch or CI log, it must be rotated.
- [ ] **[Medium] No certificate pinning** on `OkHttpClient`. Not required for a challenge, worth noting.
- [ ] **[Medium] No network security config** (`network_security_config.xml`) — cleartext HTTP is blocked on targetSdk 36 by default, but explicit config would be more defensive.
- [ ] **[Low] `android:exported="true"` on `MainActivity`** with a deep-link `intent-filter` — required for API 31+, fine. `autoVerify="false"` is correct since we're not using HTTPS links.
- [ ] **[Low] `android:allowBackup="true"`** — may back up user favorites/settings. Acceptable for a challenge; a production app might opt out or ship `backup_rules.xml`.

---

## 10. Documentation & DX

- [x] `README.md` is thorough, covers setup, variants, testing, quality gates, and assumptions.
- [x] `ARCHITECTURE.md` has the module graph (mermaid), layering diagram, navigation, theming, data flow sequence diagram, testing matrix, build notes, and known limitations.
- [x] "Areas for Future Improvement" section is candid about deep-link manifest gap (now fixed), Paging 3, Crashlytics, proxy, etc.
- [ ] **[High] Docs say things that the code does not.** "pull-to-refresh" (not wired), "release (proguard-ready)" (not minified), "App-level UI smoke + navigation" (test only launches), "`connectedDevDebugAndroidTest` in `:app`" (will fail without HiltTestRunner).
- [ ] **[Medium] No CI config** (`.github/workflows/`, Bitrise, etc.). All quality gates are invocable locally only.
- [ ] **[Low] `ARCHITECTURE.md`'s mermaid** has `settings --> designsystem` but the `SettingsRepository` in `:feature:settings` also reads `ThemeMode` from `:core:designsystem` — confirming the coupling issue called out in §4.

---

## 11. Feature gap fixes — minimum-viable patch list

Roughly ordered by impact. Each item maps to a finding above.

1. **Unify settings DataStore.** Delete `:app/settings/SettingsManager` and inject `SettingsRepository` into `MainActivity` through Hilt-accessible `EntryPointAccessors` or expose a Composition local, then read `themeMode` / `dynamicColor` from the single source. (§2 Critical #1)
2. **Wire `HiltTestRunner`.** Change `testInstrumentationRunner` to `"nl.ing.assessment.recipes.HiltTestRunner"` in `:app/build.gradle.kts`, `:benchmark/build.gradle.kts`, and `RecipesAndroidLibraryPlugin`. (§2 Critical #2, §7 Critical)
3. **Set `testTagsAsResourceId = true`** on the root `Box` in `MainActivity.setContent { … }`. (§3 Critical, §8 Critical)
4. **Declare `benchmark` build type on `:app`**, `initWith(release)`, `isDebuggable = true`, then flip `isMinifyEnabled = true` on `release`, `shrinkResources = true`, and add `implementation(libs.androidx.profileinstaller)`. Apply `androidx.baselineprofile` plugin. (§2 High, §8 High)
5. **Move `Timber.plant` out of `IngRecipesApplication`.** Let the source-set-split `LoggingModule` own planting; `TimberLogger.init {}` already guards `treeCount == 0`. (§2 High)
6. **Centralise `Throwable.toUiText()`** in `:core:domain/mapper/ErrorKindMapper.kt`. Delete the three copies and unify the fallback string. (§4 High)
7. **Add real Compose UI tests** for each stateless `*Screen` — one "renders list", one "clicks card", one "dismisses error", one "empty state" per feature. Target `src/androidTest` with `createComposeRule()`. (§7 Critical)
8. **Extract `SearchViewModel.performSearch` helpers** — `handleBlankQuery`, `applySuccess`, `applyError`. (§5 High)
9. **Wire `PullToRefreshBox`** in `SearchScreen` driven by `state.isRefreshing`, calling `onRefresh`. (§3 High)
10. **Handle offline-with-cache** — when `searchRecipes()` throws `IOException` and `state.recipes.isNotEmpty()`, emit a side-effect snackbar instead of flipping to the full-screen error state. (§6 High)
11. **Rotate the committed-on-disk Spoonacular API key.** (§9 High)

---

## 12. Severity counts

| Severity | Count |
|---|---|
| Critical | 6 |
| High | 22 |
| Medium | 24 |
| Low | 11 |

**Overall verdict:** The project is a strong staff-level starter that demonstrates the *shape* of a production Android codebase — modularisation, Hilt DI, Compose MVVM, Room offline-first, Navigation 3, Macrobenchmark, Detekt, JaCoCo aggregate. But several of the project's own claims — settings persistence, pull-to-refresh, minified release, Hilt-powered instrumentation tests, working baseline profile, working benchmark scroll — are **not reflected in the source**. Fixing the six Critical items is roughly a day of work; the Highs are another day or two. After that, the codebase would genuinely match the standard its skill documentation sets.
