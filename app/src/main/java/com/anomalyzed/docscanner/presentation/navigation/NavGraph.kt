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
    object Result : Screen("result/{pdfUri}/{imageUris}") {
        fun createRoute(pdfUri: String, imageUris: List<String>): String {
            val encodedPdf = Uri.encode(pdfUri.ifEmpty { "none" })
            val joinedImages = if (imageUris.isEmpty()) "none" else imageUris.joinToString(",")
            val encodedImages = Uri.encode(joinedImages)
            return "result/$encodedPdf/$encodedImages"
        }
    }
}

@Composable
fun DocScannerNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onScanSuccess = { pdfUri, imageUris ->
                    navController.navigate(Screen.Result.createRoute(pdfUri, imageUris))
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
                navArgument("imageUris") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pdfUriString = backStackEntry.arguments?.getString("pdfUri")
            val imageUrisString = backStackEntry.arguments?.getString("imageUris")
            val pdfUri = pdfUriString?.takeIf { it != "none" }?.let { Uri.parse(Uri.decode(it)) }
            val imageUris = imageUrisString?.takeIf { it != "none" }?.let { 
                Uri.decode(it).split(",").map { uri -> Uri.parse(uri) }
            } ?: emptyList()

            ResultScreen(
                pdfUri = pdfUri,
                imageUris = imageUris,
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
