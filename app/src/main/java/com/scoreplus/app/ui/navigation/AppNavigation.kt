package com.scoreplus.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.scoreplus.app.ScorePlusApp
import com.scoreplus.app.ui.screens.addexpense.AddExpenseScreen
import com.scoreplus.app.ui.screens.auth.LoginScreen
import com.scoreplus.app.ui.screens.auth.RegisterScreen
import com.scoreplus.app.ui.screens.categories.CategoryScreen
import com.scoreplus.app.ui.screens.history.HistoryScreen
import com.scoreplus.app.ui.screens.home.HomeScreen
import com.scoreplus.app.ui.screens.profile.ProfileScreen
import com.scoreplus.app.ui.screens.yearly.YearlyScreen

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Anasayfa", Icons.Default.Home)
    object History : BottomNavItem("history", "Geçmiş", Icons.Default.DateRange)
    object Yearly : BottomNavItem("yearly", "Yıllık", Icons.Default.Star)
    object Categories : BottomNavItem("categories", "Kategoriler", Icons.Default.List)
    object Profile : BottomNavItem("profile", "Hesap", Icons.Default.Person)
}

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.History,
    BottomNavItem.Yearly,
    BottomNavItem.Categories,
    BottomNavItem.Profile
)

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val tokenStore = (context.applicationContext as ScorePlusApp).tokenStore
    val shouldShowHome by tokenStore.shouldShowHome.collectAsState(initial = false)
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route?.startsWith("add_expense") == false &&
        currentDestination?.route?.startsWith("month_detail") == false &&
        currentDestination?.route != "login" &&
        currentDestination?.route != "register"

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(BottomNavItem.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (shouldShowHome) BottomNavItem.Home.route else "login",
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate("register") },
                    onContinueAsGuest = {
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }
            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    onNavigateToAddExpense = { month, year ->
                        navController.navigate("add_expense/$month/$year")
                    }
                )
            }
            composable(BottomNavItem.History.route) {
                HistoryScreen(onMonthClick = { month, year ->
                    navController.navigate("month_detail/$month/$year")
                })
            }
            composable(
                route = "month_detail/{month}/{year}",
                arguments = listOf(
                    navArgument("month") { type = NavType.IntType },
                    navArgument("year") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val month = backStackEntry.arguments!!.getInt("month")
                val year = backStackEntry.arguments!!.getInt("year")
                HomeScreen(
                    initialMonth = month,
                    initialYear = year,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddExpense = { m, y -> navController.navigate("add_expense/$m/$y") }
                )
            }
            composable(BottomNavItem.Yearly.route) {
                YearlyScreen()
            }
            composable(BottomNavItem.Categories.route) {
                CategoryScreen()
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onNavigateToLogin = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = "add_expense/{month}/{year}",
                arguments = listOf(
                    navArgument("month") { type = NavType.IntType },
                    navArgument("year") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val month = backStackEntry.arguments?.getInt("month") ?: 1
                val year = backStackEntry.arguments?.getInt("year") ?: 2026
                AddExpenseScreen(
                    month = month,
                    year = year,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
