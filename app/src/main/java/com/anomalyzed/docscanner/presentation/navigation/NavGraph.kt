package com.anomalyzed.docscanner.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.anomalyzed.docscanner.presentation.home.HomeScreen
import com.anomalyzed.docscanner.presentation.result.ResultScreen
import com.anomalyzed.docscanner.presentation.scans.ScansScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Scans : Screen("scans")
    object Result : Screen("result/{pdfUri}/{imageUri}") {
        fun createRoute(pdfUri: String, imageUri: String): String {
            val encodedPdf = Uri.encode(pdfUri.ifEmpty { "none" })
            val encodedImage = Uri.encode(imageUri.ifEmpty { "none" })
            return "result/$encodedPdf/$encodedImage"
        }
    }
}

@Composable
fun DocScannerNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onScanSuccess = { pdfUri, imageUri ->
                    navController.navigate(Screen.Result.createRoute(pdfUri, imageUri))
                },
                onNavigateToScans = {
                    navController.navigate(Screen.Scans.route)
                }
            )
        }

        composable(Screen.Scans.route) {
            ScansScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.Result.route,
            arguments = listOf(
                navArgument("pdfUri") { type = NavType.StringType },
                navArgument("imageUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pdfUriString = backStackEntry.arguments?.getString("pdfUri")
            val imageUriString = backStackEntry.arguments?.getString("imageUri")
            val pdfUri = pdfUriString?.takeIf { it != "none" }?.let { Uri.parse(Uri.decode(it)) }
            val imageUri = imageUriString?.takeIf { it != "none" }?.let { Uri.parse(Uri.decode(it)) }

            ResultScreen(
                pdfUri = pdfUri,
                imageUri = imageUri,
                onNavigateToScans = {
                    navController.navigate(Screen.Scans.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }
    }
}
