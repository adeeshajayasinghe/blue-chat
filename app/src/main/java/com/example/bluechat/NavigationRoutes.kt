package com.example.bluechat

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

object NavigationRoutes {
    // Simple routes
    const val FRONT_PAGE = "frontPage"
    const val MAIN_SCREEN = "mainScreen"
    const val SAMPLE_DEMO_ROUTE = "sampleDemo"

    // Route with parameters
    const val SAMPLE_DEMO_ID = "sampleDemoId"
    const val SAMPLE_DEMO_FULL_ROUTE = "$SAMPLE_DEMO_ROUTE/{$SAMPLE_DEMO_ID}"

    // Arguments for parameterized routes
    val sampleDemoArguments: List<NamedNavArgument>
        get() = listOf(
            navArgument(SAMPLE_DEMO_ID) { type = NavType.StringType }
        )

    // Sealed class for type-safe routes
    sealed class Screen(val route: String) {
        object FrontPage : Screen(FRONT_PAGE)
        object MainScreen : Screen(MAIN_SCREEN)
        object SampleDemo : Screen(SAMPLE_DEMO_FULL_ROUTE)
    }
}