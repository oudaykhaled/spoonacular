package nl.ing.assessment.recipes.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import nl.ing.assessment.recipes.R
import nl.ing.assessment.recipes.core.designsystem.component.ConnectivityBanner
import nl.ing.assessment.recipes.feature.details.presentation.DetailsRoute
import nl.ing.assessment.recipes.feature.favorites.presentation.FavoritesRoute
import nl.ing.assessment.recipes.feature.search.presentation.SearchRoute
import nl.ing.assessment.recipes.feature.settings.presentation.SettingsRoute

private enum class BottomTab(
    val route: NavKey,
    @StringRes val labelRes: Int,
    val testTag: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    SEARCH(
        route = AppRoute.Search,
        labelRes = R.string.tab_search,
        testTag = "nav_search",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
    ),
    FAVORITES(
        route = AppRoute.Favorites,
        labelRes = R.string.tab_favorites,
        testTag = "nav_favorites",
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder,
    ),
    SETTINGS(
        route = AppRoute.Settings,
        labelRes = R.string.tab_settings,
        testTag = "nav_settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    ),
}

@Suppress("LongMethod")
@Composable
fun AppNavigation(
    initialDeepLinkRecipeId: Int? = null,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(AppRoute.Search)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialDeepLinkRecipeId) {
        if (initialDeepLinkRecipeId != null) {
            backStack.add(AppRoute.Details(initialDeepLinkRecipeId))
        }
    }

    val showSnackbar: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    val currentRoot: NavKey = backStack.firstOrNull() ?: AppRoute.Search
    val isOnRoot = backStack.size <= 1

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ConnectivityBanner()
        },
        bottomBar = {
            if (isOnRoot) {
                NavigationBar(modifier = Modifier.testTag("bottom_navigation_bar")) {
                    BottomTab.entries.forEach { tab ->
                        val selected = currentRoot == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    backStack.apply {
                                        clear()
                                        add(tab.route)
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                            modifier = Modifier.testTag(tab.testTag),
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            entryProvider = entryProvider {
                entry<AppRoute.Search> {
                    SearchRoute(
                        onNavigateToDetails = { id -> backStack.add(AppRoute.Details(id)) },
                        onShowSnackbar = showSnackbar,
                    )
                }
                entry<AppRoute.Favorites> {
                    FavoritesRoute(
                        onNavigateToDetails = { id -> backStack.add(AppRoute.Details(id)) },
                        onShowSnackbar = showSnackbar,
                    )
                }
                entry<AppRoute.Settings> {
                    SettingsRoute(
                        onBack = {
                            if (backStack.size > 1) backStack.removeLastOrNull()
                        },
                    )
                }
                entry<AppRoute.Details> { route ->
                    DetailsRoute(
                        recipeId = route.recipeId,
                        onBack = {
                            if (backStack.size > 1) backStack.removeLastOrNull()
                        },
                        onShowSnackbar = showSnackbar,
                    )
                }
            },
        )
    }
}
