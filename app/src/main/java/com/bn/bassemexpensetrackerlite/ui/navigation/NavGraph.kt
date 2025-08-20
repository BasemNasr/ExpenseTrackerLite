package com.bn.bassemexpensetrackerlite.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bn.bassemexpensetrackerlite.ui.screens.AddExpenseScreen
import com.bn.bassemexpensetrackerlite.ui.screens.DashboardScreen

object Routes {
    const val DASHBOARD: String = "dashboard"
    const val ADD_EXPENSE: String = "add_expense"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {
        composable(
            route = Routes.DASHBOARD,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            }
        ) {
            DashboardScreen(
                navController = navController,
                onAddClicked = { navController.navigate(Routes.ADD_EXPENSE) }
            )
        }
        composable(
            route = Routes.ADD_EXPENSE,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            AddExpenseScreen(onBack = {
                navController.previousBackStackEntry?.savedStateHandle?.set("expense_added", System.currentTimeMillis())
                navController.popBackStack()
            })
        }
    }
}


