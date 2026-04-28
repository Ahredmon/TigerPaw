package com.tigerpaw.launcher.navigation

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tigerpaw.launcher.feature.home.HomeScreen
import com.tigerpaw.launcher.feature.settings.InsightsScreen
import com.tigerpaw.launcher.feature.settings.PinnedAppsScreen
import com.tigerpaw.launcher.feature.settings.SettingsScreen
import com.tigerpaw.launcher.feature.widgets.WidgetsScreen

sealed class LauncherDestination(val route: String) {
    data object Home : LauncherDestination("home")
    data object Widgets : LauncherDestination("widgets")
    data object Settings : LauncherDestination("settings")
    data object Insights : LauncherDestination("insights")
    data object PinnedApps : LauncherDestination("pinned_apps")
}

// Fast easeOut slide — crisp, no overshoot, finishes in ~280ms
private val SLIDE_DURATION = 280
private val FADE_DURATION = 200
private val SlideEasing: Easing = FastOutSlowInEasing

private val SlideUpEnter = slideInVertically(
    initialOffsetY = { it },
    animationSpec = tween(SLIDE_DURATION, easing = SlideEasing),
) + fadeIn(tween(FADE_DURATION, easing = SlideEasing))

private val SlideDownExit = slideOutVertically(
    targetOffsetY = { it },
    animationSpec = tween(SLIDE_DURATION, easing = SlideEasing),
) + fadeOut(tween(FADE_DURATION, easing = SlideEasing))

private val FadeIn = fadeIn(tween(180, easing = SlideEasing))
private val FadeOut = fadeOut(tween(140, easing = SlideEasing))

@Composable
fun TigerPawNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = LauncherDestination.Home.route,
        enterTransition = { FadeIn },
        exitTransition = { FadeOut },
        popEnterTransition = { FadeIn },
        popExitTransition = { FadeOut },
    ) {
        composable(LauncherDestination.Home.route) {
            HomeScreen(
                onOpenSettings = { navController.navigate(LauncherDestination.Settings.route) },
                onOpenWidgets = { navController.navigate(LauncherDestination.Widgets.route) },
            )
        }
        composable(
            route = LauncherDestination.Widgets.route,
            enterTransition = { SlideUpEnter },
            popExitTransition = { SlideDownExit },
        ) {
            WidgetsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = LauncherDestination.Settings.route,
            enterTransition = { SlideUpEnter },
            popExitTransition = { SlideDownExit },
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenInsights = { navController.navigate(LauncherDestination.Insights.route) },
                onOpenPinnedApps = { navController.navigate(LauncherDestination.PinnedApps.route) },
            )
        }
        composable(
            route = LauncherDestination.Insights.route,
            enterTransition = { SlideUpEnter },
            popExitTransition = { SlideDownExit },
        ) {
            InsightsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = LauncherDestination.PinnedApps.route,
            enterTransition = { SlideUpEnter },
            popExitTransition = { SlideDownExit },
        ) {
            PinnedAppsScreen(onBack = { navController.popBackStack() })
        }
    }
}

