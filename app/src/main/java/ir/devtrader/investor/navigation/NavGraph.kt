package ir.devtrader.investor.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ir.devtrader.investor.AppContainer
import ir.devtrader.investor.R
import ir.devtrader.investor.ui.alarms.AlarmsScreen
import ir.devtrader.investor.ui.dashboard.DashboardScreen
import ir.devtrader.investor.ui.debtledger.DebtLedgerScreen
import ir.devtrader.investor.ui.login.LoginScreen
import ir.devtrader.investor.ui.notifications.NotificationsScreen
import ir.devtrader.investor.ui.positions.PositionsScreen
import ir.devtrader.investor.ui.profile.ProfileScreen
import ir.devtrader.investor.ui.register.RegisterScreen
import ir.devtrader.investor.ui.settings.SettingsScreen
import ir.devtrader.investor.ui.shell.AppShell
import ir.devtrader.investor.ui.trades.TradesScreen
import kotlinx.coroutines.launch

@Composable
fun TradeBotNavGraph(appContainer: AppContainer) {
    val navController = rememberNavController()
    val isLoggedIn by appContainer.sessionManager.isLoggedIn.collectAsState()
    val investor by appContainer.sessionManager.currentInvestor.collectAsState()
    val unseenCount by appContainer.notificationsCenter.unseenCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val investorInitial = investor?.first?.trim()?.firstOrNull()?.uppercase() ?: "?"

    // No refresh-token flow exists yet: any 401 clears the session, and this is the one
    // place that reacts by bouncing the whole app back to Login. It also drives the forward
    // transition into the app after a successful Login/Register.
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate(Destinations.DASHBOARD) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            navController.navigateToLoginAndClearBackStack()
        }
    }

    // App/session-level handling of the two socket events, independent of whatever screen is
    // currently open — a toast for a triggered alarm must still show even off the Alarms screen.
    val notificationToastFormat = stringResource(R.string.notifications_toast_format)
    val alarmToastFormat = stringResource(R.string.alarm_triggered_toast_format)
    LaunchedEffect(Unit) {
        launch {
            appContainer.realtimeGateway.notifications.collect { event ->
                snackbarHostState.showSnackbar(notificationToastFormat.format(event.title, event.message))
            }
        }
        launch {
            appContainer.realtimeGateway.alarmTriggered.collect { event ->
                snackbarHostState.showSnackbar(alarmToastFormat.format(event.symbol, event.condition, event.price))
            }
        }
    }

    @Composable
    fun shell(title: String, route: String, showBack: Boolean = false, content: @Composable (PaddingValues) -> Unit) {
        AppShell(
            title = title,
            currentRoute = route,
            navController = navController,
            investorInitial = investorInitial,
            unseenCount = unseenCount,
            onLogout = { appContainer.authRepository.logout() },
            snackbarHostState = snackbarHostState,
            showBack = showBack,
            content = content,
        )
    }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Destinations.DASHBOARD else Destinations.LOGIN,
    ) {
        composable(Destinations.LOGIN) {
            LoginScreen(
                authRepository = appContainer.authRepository,
                onOpenRegister = { navController.navigate(Destinations.REGISTER) },
            )
        }
        composable(Destinations.REGISTER) {
            RegisterScreen(
                authRepository = appContainer.authRepository,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Destinations.DASHBOARD) {
            shell(stringResource(R.string.nav_dashboard), Destinations.DASHBOARD) { padding ->
                DashboardScreen(
                    investorRepository = appContainer.investorRepository,
                    onOpenSettings = {
                        navController.navigate(Destinations.SETTINGS) {
                            popUpTo(Destinations.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.padding(padding),
                )
            }
        }
        composable(Destinations.POSITIONS) {
            shell(stringResource(R.string.nav_positions), Destinations.POSITIONS) { padding ->
                PositionsScreen(appContainer.investorRepository, Modifier.padding(padding))
            }
        }
        composable(Destinations.TRADES) {
            shell(stringResource(R.string.nav_trades), Destinations.TRADES) { padding ->
                TradesScreen(appContainer.investorRepository, Modifier.padding(padding))
            }
        }
        composable(Destinations.DEBT_LEDGER) {
            shell(stringResource(R.string.nav_debt_ledger), Destinations.DEBT_LEDGER) { padding ->
                DebtLedgerScreen(appContainer.investorRepository, Modifier.padding(padding))
            }
        }
        composable(Destinations.ALARMS) {
            shell(stringResource(R.string.nav_alarms_title), Destinations.ALARMS) { padding ->
                AlarmsScreen(
                    investorRepository = appContainer.investorRepository,
                    realtimeGateway = appContainer.realtimeGateway,
                    symbolsCache = appContainer.symbolsCache,
                    modifier = Modifier.padding(padding),
                )
            }
        }
        composable(Destinations.PROFILE) {
            shell(stringResource(R.string.nav_profile), Destinations.PROFILE) { padding ->
                ProfileScreen(appContainer.investorRepository, Modifier.padding(padding))
            }
        }
        composable(Destinations.SETTINGS) {
            shell(stringResource(R.string.nav_settings), Destinations.SETTINGS) { padding ->
                SettingsScreen(appContainer.investorRepository, Modifier.padding(padding))
            }
        }
        composable(Destinations.NOTIFICATIONS) {
            shell(stringResource(R.string.nav_notifications_title), Destinations.NOTIFICATIONS, showBack = true) { padding ->
                NotificationsScreen(appContainer.notificationsCenter, Modifier.padding(padding))
            }
        }
    }
}

private fun NavHostController.navigateToLoginAndClearBackStack() {
    navigate(Destinations.LOGIN) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}
