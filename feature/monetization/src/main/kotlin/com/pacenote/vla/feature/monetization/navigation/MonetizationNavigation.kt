package com.pacenote.vla.feature.monetization.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.pacenote.vla.feature.monetization.ui.MonetizationScreen

const val MONETIZATION_ROUTE = "monetization"

fun NavGraphBuilder.monetizationGraph(navController: NavController) {
    composable(MONETIZATION_ROUTE) {
        MonetizationScreen(
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }
}
