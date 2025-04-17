package com.example.bluechat

import android.content.Intent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavView() {
    val context = LocalContext.current
    val navController = rememberNavController()

    AppTheme {
        NavHost(
            navController = navController,
            startDestination = NavigationRoutes.FRONT_PAGE,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(500)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(500)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(500)
                )
            }
        ) {
            composable(NavigationRoutes.FRONT_PAGE) {
                FrontPageScreen(
                    onGetStartedClick = {
                        navController.navigate(MainScreen) {
                            // Optional: This ensures front page is removed from back stack
                            popUpTo(NavigationRoutes.FRONT_PAGE) { inclusive = true }
                        }
                    }
                )
            }
            composable<MainScreen> {
                CatalogScreen(
                    item = MainScreen,
                    subItems = BLE_OPERATIONS,
                    onNavigateToSubItem = { sampleDemo ->
                        if (sampleDemo is ActivitySampleDemo) {
                            context.startActivity(Intent(context, sampleDemo.content))
                        } else {
                            navController.navigate("${NavigationRoutes.SAMPLE_DEMO_ROUTE}/${sampleDemo.id}")
                        }
                    },
                )
            }
            composable(
                NavigationRoutes.SAMPLE_DEMO_FULL_ROUTE,
                arguments = NavigationRoutes.sampleDemoArguments,
            ) { backStackEntry ->
                val arguments = requireNotNull(backStackEntry.arguments)
                val sampleDemoId = requireNotNull(arguments.getString(NavigationRoutes.SAMPLE_DEMO_ID))
                val sampleDemo = BLE_OPERATIONS.getValue(sampleDemoId)

                Scaffold(
                    topBar = { TopAppBar(title = { Text(sampleDemo.name) }) },
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .padding(paddingValues)
                            .padding(8.dp)
                            .fillMaxSize(),
                    ) {
                        sampleDemo.content()
                    }
                }
            }
        }
    }
}

@Serializable
data object MainScreen : CatalogItem {
    override val id = "main"
    override val name = "BlueChat"
    override val description = null
}