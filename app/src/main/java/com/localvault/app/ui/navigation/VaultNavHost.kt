package com.localvault.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.localvault.app.VaultSession
import com.localvault.app.data.PasswordRepository
import com.localvault.app.ui.screens.EntryDetailScreen
import com.localvault.app.ui.screens.EntryEditScreen
import com.localvault.app.ui.screens.GeneratorScreen
import com.localvault.app.ui.screens.HomeScreen
import com.localvault.app.ui.screens.SettingsScreen

@Composable
fun VaultNavHost(session: VaultSession, onLock: () -> Unit) {
    val navController = rememberNavController()
    val repository = PasswordRepository(session)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomRoutes = listOf(BottomTab.Home, BottomTab.Generator, BottomTab.Settings)
    val showBottomBar = bottomRoutes.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomRoutes.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    launchSingleTop = true
                                    popUpTo("home") { saveState = true }
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") {
                HomeScreen(
                    repository = repository,
                    onOpenEntry = { id -> navController.navigate("detail/$id") },
                    onAddEntry = { navController.navigate("edit/new") },
                    onSettings = { navController.navigate("settings") },
                    onLock = onLock,
                )
            }
            composable("generator") {
                GeneratorScreen()
            }
            composable("settings") {
                SettingsScreen(
                    session = session,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                EntryDetailScreen(
                    entryId = id,
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("edit/$id") },
                )
            }
            composable(
                route = "edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                EntryEditScreen(
                    entryId = if (id == "new") null else id,
                    repository = repository,
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}

private enum class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "Vault", Icons.Default.Lock),
    Generator("generator", "Generator", Icons.Default.Key),
    Settings("settings", "Settings", Icons.Default.Settings),
}
