package ir.devtrader.investor.data.remote

import ir.devtrader.investor.data.remote.dto.AlarmsResponse
import ir.devtrader.investor.data.remote.dto.BindKeyRequest
import ir.devtrader.investor.data.remote.dto.BindKeyResponse
import ir.devtrader.investor.data.remote.dto.CreateAlarmRequest
import ir.devtrader.investor.data.remote.dto.CreateAlarmResponse
import ir.devtrader.investor.data.remote.dto.DashboardResponse
import ir.devtrader.investor.data.remote.dto.DebtLedgerResponse
import ir.devtrader.investor.data.remote.dto.LoginRequest
import ir.devtrader.investor.data.remote.dto.LoginResponse
import ir.devtrader.investor.data.remote.dto.NotificationsResponse
import ir.devtrader.investor.data.remote.dto.PositionsResponse
import ir.devtrader.investor.data.remote.dto.ProfileRequest
import ir.devtrader.investor.data.remote.dto.ProfileResponse
import ir.devtrader.investor.data.remote.dto.MarginRatioRequest
import ir.devtrader.investor.data.remote.dto.MarginRatioResponse
import ir.devtrader.investor.data.remote.dto.RegisterRequest
import ir.devtrader.investor.data.remote.dto.RegisterResponse
import ir.devtrader.investor.data.remote.dto.SimpleStatusResponse
import ir.devtrader.investor.data.remote.dto.SymbolsResponse
import ir.devtrader.investor.data.remote.dto.ToggleTradingResponse
import ir.devtrader.investor.data.remote.dto.TradesResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("dashboard")
    suspend fun getDashboard(): Response<DashboardResponse>

    @GET("positions")
    suspend fun getPositions(): Response<PositionsResponse>

    @GET("trades")
    suspend fun getTrades(): Response<TradesResponse>

    @POST("profile")
    suspend fun updateProfile(@Body request: ProfileRequest): Response<ProfileResponse>

    @GET("debt-ledger")
    suspend fun getDebtLedger(): Response<DebtLedgerResponse>

    @GET("notifications")
    suspend fun getNotifications(): Response<NotificationsResponse>

    @POST("notifications/seen")
    suspend fun markNotificationsSeen(): Response<SimpleStatusResponse>

    @POST("bind-key")
    suspend fun bindKey(@Body request: BindKeyRequest): Response<BindKeyResponse>

    @POST("settings/trading-enabled")
    suspend fun toggleTradingEnabled(): Response<ToggleTradingResponse>

    @POST("settings/margin-ratio")
    suspend fun updateMarginRatio(@Body request: MarginRatioRequest): Response<MarginRatioResponse>

    @GET("alarms")
    suspend fun getAlarms(): Response<AlarmsResponse>

    @POST("alarms")
    suspend fun createAlarm(@Body request: CreateAlarmRequest): Response<CreateAlarmResponse>

    @POST("alarms/{id}/delete")
    suspend fun deleteAlarm(@Path("id") id: String): Response<SimpleStatusResponse>

    @GET("symbols")
    suspend fun getSymbols(): Response<SymbolsResponse>
}
