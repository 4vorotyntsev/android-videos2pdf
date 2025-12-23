package com.vs.videoscanpdf.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vs.videoscanpdf.ui.exports.ExportsLibraryScreen
import com.vs.videoscanpdf.ui.help.HelpScreen
import com.vs.videoscanpdf.ui.home.HomeScreen

/**
 * Bottom navigation destinations.
 * 
 * Updated to: Home | Exports | Help
 * (Removed Projects tab, added Exports Library)
 */
enum class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(
        route = "tab_home",
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    EXPORTS(
        route = "tab_exports",
        label = "Exports",
        selectedIcon = Icons.Filled.FolderOpen,
        unselectedIcon = Icons.Outlined.FolderOpen
    ),
    HELP(
        route = "tab_help",
        label = "Help",
        selectedIcon = Icons.Filled.Help,
        unselectedIcon = Icons.Outlined.Help
    )
}

/**
 * Main scaffold containing bottom navigation and tab content.
 * 
 * This is the main container after splash/onboarding.
 * Contains three tabs: Home, Exports, Help
 */
@Composable
fun MainScaffold(
    onNavigateToRecorder: (sessionId: String) -> Unit,
    onNavigateToImport: (sessionId: String) -> Unit,
    onNavigateToTrim: (sessionId: String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExportResult: (sessionId: String) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.entries.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.HOME.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavItem.HOME.route) {
                HomeScreen(
                    onStartScanning = onNavigateToRecorder,
                    onImportVideo = onNavigateToImport,
                    onSettingsClick = onNavigateToSettings,
                    onExportClick = onNavigateToExportResult
                )
            }
            
            composable(BottomNavItem.EXPORTS.route) {
                ExportsLibraryScreen(
                    onExportClick = onNavigateToExportResult
                )
            }
            
            composable(BottomNavItem.HELP.route) {
                HelpScreen(
                    onSettingsClick = onNavigateToSettings
                )
            }
        }
    }
}
