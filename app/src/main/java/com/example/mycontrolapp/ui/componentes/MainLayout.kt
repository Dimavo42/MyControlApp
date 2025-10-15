@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.mycontrolapp.ui.componentes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapp.appNavigation.AppDestinations
import com.example.mycontrolapp.appNavigation.BottomNavBar
import com.example.mycontrolapp.appNavigation.BottomNavItem
import com.example.mycontrolapp.ui.componentes.screens.AddActivity
import com.example.mycontrolapp.ui.componentes.screens.AddUser
import com.example.mycontrolapp.ui.componentes.screens.AssignmentScreen
import com.example.mycontrolapp.ui.componentes.screens.HomeScreen
import com.example.mycontrolapp.ui.componentes.screens.SavedActivities
import com.example.mycontrolapp.R

@Composable
fun MainLayout() {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem(stringResource(R.string.nav_home), Icons.Default.Home, AppDestinations.Home),
        BottomNavItem(stringResource(R.string.nav_add_activity), Icons.Default.Add, AppDestinations.Activity),
        BottomNavItem(stringResource(R.string.nav_add_user), Icons.Default.Person, AppDestinations.AddUser),
        BottomNavItem(stringResource(R.string.nav_activities), Icons.Default.Done, AppDestinations.SavedActivities)
    )

    fun baseRoute(route: String?): String? = route?.substringBefore("?")

    Scaffold(
        modifier = Modifier.testTag("scaffoldMainLayout"),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_title),
                        modifier = Modifier.testTag("topAppBarTitle")
                    )
                },
                modifier = Modifier.testTag("topAppBar")
            )
        },
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDest = backStackEntry?.destination
            val hideBottomBar =
                currentDest?.hierarchy?.any {
                    val r = baseRoute(it.route)
                    r?.startsWith("${AppDestinations.Assignment}/") == true
                } == true

            if (!hideBottomBar) {
                BottomNavBar(
                    navController = navController,
                    items = bottomNavItems,
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestinations.Home,
            modifier = Modifier
                .padding(innerPadding)
                .testTag("navHost")
        ) {
            // Tabs
            composable(AppDestinations.Home) {
                HomeScreen(
                    navController = navController,
                    modifier = Modifier.testTag("screenHome")
                )
            }
            composable(AppDestinations.Activity) {
                // Plain tab -> empty form
                AddActivity(
                    navController = navController,
                    modifier = Modifier.testTag("screenAddActivity")
                )
            }
            composable(AppDestinations.AddUser) {
                AddUser(
                    navController = navController,
                    modifier = Modifier.testTag("screenAddUser")
                )
            }
            composable(AppDestinations.SavedActivities) {
                SavedActivities(
                    navController = navController,
                    modifier = Modifier.testTag("screenSavedActivities")
                )
            }

            // Assignment/{activityId} (hide bottom bar)
            composable(
                route = "${AppDestinations.Assignment}/{activityId}",
                arguments = listOf(navArgument("activityId") { type = NavType.StringType })
            ) { backStackEntry ->
                val activityId = backStackEntry.arguments?.getString("activityId") ?: return@composable
                AssignmentScreen(
                    navController = navController,
                    activityId = activityId
                )
            }

            composable(
                route = "${AppDestinations.ActivityWithDate}/{dateEpochDay}",
                arguments = listOf(navArgument("dateEpochDay") { type = NavType.LongType })
            ) { backStackEntry ->
                val dateEpochDay = backStackEntry.arguments?.getLong("dateEpochDay")
                AddActivity(
                    navController = navController,
                    prefillEpochDay = dateEpochDay,
                    modifier = Modifier.testTag("screenAddActivityForDate")
                )
            }
        }
    }
}


