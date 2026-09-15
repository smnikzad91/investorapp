package ir.devtrader.investor

import android.content.Context
import ir.devtrader.investor.data.local.SessionManager
import ir.devtrader.investor.data.local.TokenManager
import ir.devtrader.investor.data.remote.NetworkModule
import ir.devtrader.investor.data.remote.RealtimeGateway
import ir.devtrader.investor.data.repository.AuthRepository
import ir.devtrader.investor.data.repository.InvestorRepository
import ir.devtrader.investor.data.repository.NotificationsCenter
import ir.devtrader.investor.data.repository.SymbolsCache
import ir.devtrader.investor.update.ApkDownloader
import ir.devtrader.investor.update.UpdateCheckCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Small manual service locator. The app is not large enough to justify Hilt (see the
 * project brief — Hilt is called out as optional), so dependencies are wired by hand here.
 */
class AppContainer(context: Context) {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /** Lives for the process lifetime, unlike a screen's viewModelScope — backs the socket connection and the notifications badge. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val sessionManager = SessionManager(TokenManager(context.applicationContext))

    private val apiService = NetworkModule.buildApiService(sessionManager)

    val authRepository = AuthRepository(apiService, sessionManager, json)
    val investorRepository = InvestorRepository(apiService, json)

    val realtimeGateway = RealtimeGateway(json)
    val notificationsCenter = NotificationsCenter(investorRepository, realtimeGateway, appScope)
    val symbolsCache = SymbolsCache(investorRepository)

    val updateApi = NetworkModule.buildUpdateApi()
    val apkDownloader = ApkDownloader(context.applicationContext)
    val updateCheckCache = UpdateCheckCache()

    init {
        // Connect the socket and warm the notifications/symbols caches whenever a session becomes
        // active, and tear the socket down on logout — mirrors the nav graph's own reaction to
        // isLoggedIn. Symbols in particular are fetched here (app start / login), not from the
        // Alarms screen, so its dropdown never waits on a network call.
        appScope.launch {
            sessionManager.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    sessionManager.token()?.let { realtimeGateway.connect(it) }
                    notificationsCenter.refresh()
                    symbolsCache.refresh()
                } else {
                    realtimeGateway.disconnect()
                }
            }
        }
    }
}
