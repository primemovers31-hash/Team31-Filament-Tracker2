package com.bambu.nfc.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bambu.nfc.ui.detail.SpoolDetailScreen
import com.bambu.nfc.ui.detail.SpoolDetailViewModel
import com.bambu.nfc.ui.inventory.InventoryScreen
import com.bambu.nfc.ui.inventory.InventoryViewModel
import com.bambu.nfc.ui.scan.ScanScreen
import com.bambu.nfc.ui.scan.ScanViewModel
import com.bambu.nfc.ui.settings.SettingsScreen

sealed class Screen(val route: String, val label: String) {
    data object Scan : Screen("scan", "Scan")
    data object Inventory : Screen("inventory", "Inventory")
    data object Settings : Screen("settings", "Settings")
    data object Detail : Screen("detail/{spoolId}", "Detail") {
        fun createRoute(spoolId: Long) = "detail/$spoolId"
    }
}

@Composable
fun AppNavGraph(
    scanViewModel: ScanViewModel,
    userName: String?,
    userEmail: String?,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomBarScreens = listOf(Screen.Scan, Screen.Inventory, Screen.Settings)
    val showBottomBar = currentDestination?.hierarchy?.any { dest ->
        bottomBarScreens.any { it.route == dest.route }
    } == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomBarScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        Screen.Scan -> Icons.Default.Nfc
                                        Screen.Inventory -> Icons.Default.Inventory2
                                        Screen.Settings -> Icons.Default.Settings
                                        Screen.Detail -> Icons.Default.Inventory2
                                    },
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == screen.route
                            } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
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
            startDestination = Screen.Scan.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Scan.route) {
                ScanScreen(viewModel = scanViewModel)
            }
            composable(Screen.Inventory.route) {
                val viewModel = hiltViewModel<InventoryViewModel>()
                InventoryScreen(
                    viewModel = viewModel,
                    onSpoolClick = { spoolId ->
                        navController.navigate(Screen.Detail.createRoute(spoolId))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    userName = userName,
                    userEmail = userEmail,
                    onSignOut = onSignOut,
                    onDeleteAccount = onDeleteAccount
                )
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("spoolId") { type = NavType.LongType })
            ) {
                val viewModel = hiltViewModel<SpoolDetailViewModel>()
                SpoolDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
