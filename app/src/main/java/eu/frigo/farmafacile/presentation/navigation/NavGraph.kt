package eu.frigo.farmafacile.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import eu.frigo.farmafacile.presentation.screens.addedit.AddEditMedicineScreen
import eu.frigo.farmafacile.presentation.screens.addedit.AddEditMedicineViewModel
import eu.frigo.farmafacile.presentation.screens.detail.ListDetailScreen
import eu.frigo.farmafacile.presentation.screens.detail.ListDetailViewModel
import eu.frigo.farmafacile.presentation.screens.dosage.DosageScreen
import eu.frigo.farmafacile.presentation.screens.dosage.DosageViewModel
import eu.frigo.farmafacile.presentation.screens.lists.ListsScreen
import eu.frigo.farmafacile.presentation.screens.lists.ListsViewModel
import eu.frigo.farmafacile.presentation.screens.scanner.ScannerScreen
import eu.frigo.farmafacile.presentation.screens.scanner.ScannerViewModel
import eu.frigo.farmafacile.presentation.screens.settings.SettingsScreen
import eu.frigo.farmafacile.presentation.screens.settings.SettingsViewModel
import eu.frigo.farmafacile.presentation.screens.sync.SyncLogsScreen
import eu.frigo.farmafacile.presentation.screens.sync.SyncLogsViewModel

@Composable
fun FarmaFacileNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Lists.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 1. Lists Screen
        composable(Screen.Lists.route) {
            val viewModel: ListsViewModel = hiltViewModel()
            ListsScreen(
                viewModel = viewModel,
                onListSelected = { listId ->
                    navController.navigate(Screen.ListDetail.createRoute(listId))
                },
                onNavigateToDosage = {
                    navController.navigate(Screen.Dosage.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // 2. List Detail Screen
        composable(
            route = Screen.ListDetail.route,
            arguments = listOf(navArgument("listId") { type = NavType.StringType })
        ) {
            val viewModel: ListDetailViewModel = hiltViewModel()
            ListDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToScanner = { listId ->
                    navController.navigate(Screen.Scanner.createRoute(listId))
                },
                onNavigateToAddManual = { listId ->
                    navController.navigate(Screen.AddEditMedicine.createRoute(listId))
                },
                onNavigateToEdit = { listId, medId ->
                    navController.navigate(Screen.AddEditMedicine.createRoute(listId, medicineId = medId))
                },
                onNavigateToSyncLogs = { listId ->
                    navController.navigate(Screen.SyncLogs.createRoute(listId))
                }
            )
        }

        // 3. Scanner Screen
        composable(
            route = Screen.Scanner.route,
            arguments = listOf(navArgument("listId") { type = NavType.StringType })
        ) {
            val viewModel: ScannerViewModel = hiltViewModel()
            ScannerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 4. Add / Edit Medicine Screen
        composable(
            route = Screen.AddEditMedicine.route,
            arguments = listOf(
                navArgument("listId") { type = NavType.StringType },
                navArgument("medicineId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("aic") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("expiry") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("lot") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("serial") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            val viewModel: AddEditMedicineViewModel = hiltViewModel()
            AddEditMedicineScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 5. Dosage Screen
        composable(Screen.Dosage.route) {
            val viewModel: DosageViewModel = hiltViewModel()
            DosageScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 6. Settings Screen
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 7. Sync Logs Screen
        composable(
            route = Screen.SyncLogs.route,
            arguments = listOf(navArgument("listId") { type = NavType.StringType })
        ) {
            val viewModel: SyncLogsViewModel = hiltViewModel()
            SyncLogsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
