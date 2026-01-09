package com.pacenote.vla.feature.auth.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.pacenote.vla.feature.auth.ui.AuthScreen

const val AUTH_ROUTE = "auth"

fun NavGraphBuilder.authGraph(
    navController: NavController
) {
    composable(AUTH_ROUTE) {
        AuthScreen(
            onAuthSuccess = {
                navController.navigate("drive_assistant") {
                    popUpTo(AUTH_ROUTE) { inclusive = true }
                }
            }
        )
    }
}
