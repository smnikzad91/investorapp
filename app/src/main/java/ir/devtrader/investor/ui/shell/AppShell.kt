package ir.devtrader.investor.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import ir.devtrader.investor.R
import ir.devtrader.investor.navigation.Destinations
import kotlinx.coroutines.launch

private data class DrawerNavItem(val destination: String, @StringRes val label: Int, val icon: ImageVector)

private val NAV_ITEMS = listOf(
    DrawerNavItem(Destinations.DASHBOARD, R.string.nav_dashboard, Icons.Filled.Dashboard),
    DrawerNavItem(Destinations.POSITIONS, R.string.nav_positions, Icons.Filled.ShowChart),
    DrawerNavItem(Destinations.TRADES, R.string.nav_trades, Icons.Filled.History),
    DrawerNavItem(Destinations.DEBT_LEDGER, R.string.nav_debt_ledger, Icons.Filled.AccountBalance),
    DrawerNavItem(Destinations.ALARMS, R.string.nav_alarms, Icons.Filled.NotificationsActive),
    DrawerNavItem(Destinations.PROFILE, R.string.nav_profile, Icons.Filled.Person),
    DrawerNavItem(Destinations.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

/**
 * Persistent-sidebar shell mirroring the web investor panel: a slide-out drawer (header with
 * avatar/notifications bell/logout, then the nav list) wrapping every screen once logged in.
 * Login/Register are pre-auth and don't use this.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    title: String,
    currentRoute: String,
    navController: NavHostController,
    investorInitial: String,
    unseenCount: Int,
    onLogout: () -> Unit,
    snackbarHostState: SnackbarHostState,
    showBack: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun navigateTo(destination: String) {
        scope.launch { drawerState.close() }
        if (destination != currentRoute) {
            navController.navigate(destination) {
                popUpTo(Destinations.DASHBOARD) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !showBack,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                investorInitial,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                        )
                        BadgedBox(badge = { if (unseenCount > 0) Badge { Text(unseenCount.toString()) } }) {
                            IconButton(onClick = { navigateTo(Destinations.NOTIFICATIONS) }) {
                                Icon(Icons.Filled.Notifications, contentDescription = stringResource(R.string.shell_notifications_cd))
                            }
                        }
                        IconButton(onClick = { scope.launch { drawerState.close() }; onLogout() }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = stringResource(R.string.shell_logout_cd))
                        }
                    }
                    HorizontalDivider()
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        NAV_ITEMS.forEach { item ->
                            NavigationDrawerItem(
                                label = { Text(stringResource(item.label)) },
                                icon = { Icon(item.icon, contentDescription = null) },
                                selected = item.destination == currentRoute,
                                onClick = { navigateTo(item.destination) },
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        if (showBack) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.shell_back_cd))
                            }
                        } else {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.shell_menu_cd))
                            }
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding -> content(padding) }
    }
}
