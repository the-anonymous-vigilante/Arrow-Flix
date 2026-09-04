package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.example.ui.home.HomeScreen
import com.example.ui.settings.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Movies : Screen("movies", "Movies", Icons.Filled.Movie)
    object Series : Screen("series", "Series", Icons.Filled.Tv)
    object Anime : Screen("anime", "Anime", Icons.Filled.Animation)
    object Trending : Screen("trending", "Trending", Icons.Filled.LocalFireDepartment)
    object Library : Screen("library", "Library", Icons.Filled.Bookmarks)
    object Settings : Screen("settings", "Settings", Icons.Filled.AccountCircle) // Not in bottom nav
}

val items = listOf(Screen.Movies, Screen.Series, Screen.Anime, Screen.Trending, Screen.Library)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavGraph() {
    val navController = rememberNavController()
    var showProfileMenu by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        NavHost(
            navController, 
            startDestination = Screen.Movies.route,
            Modifier.padding(innerPadding),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
        ) {
            composable(Screen.Movies.route) { HomeScreen() }
            composable(Screen.Series.route) { HomeScreen() }
            composable(Screen.Anime.route) { HomeScreen() }
            composable(Screen.Trending.route) { HomeScreen() }
            composable(Screen.Library.route) { HomeScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
