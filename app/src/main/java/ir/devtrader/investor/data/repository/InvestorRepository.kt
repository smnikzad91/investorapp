package ir.devtrader.investor.data.repository

import ir.devtrader.investor.data.remote.ApiService
import ir.devtrader.investor.data.remote.dto.AlarmsResponse
import ir.devtrader.investor.data.remote.dto.BindKeyRequest
import ir.devtrader.investor.data.remote.dto.BindKeyResponse
import ir.devtrader.investor.data.remote.dto.CreateAlarmRequest
import ir.devtrader.investor.data.remote.dto.CreateAlarmResponse
import ir.devtrader.investor.data.remote.dto.DashboardResponse
import ir.devtrader.investor.data.remote.dto.DebtLedgerResponse
import ir.devtrader.investor.data.remote.dto.MarginRatioRequest
import ir.devtrader.investor.data.remote.dto.MarginRatioResponse
import ir.devtrader.investor.data.remote.dto.NotificationsResponse
import ir.devtrader.investor.data.remote.dto.PositionsResponse
import ir.devtrader.investor.data.remote.dto.ProfileRequest
import ir.devtrader.investor.data.remote.dto.ProfileResponse
import ir.devtrader.investor.data.remote.dto.SymbolsResponse
import ir.devtrader.investor.data.remote.dto.ToggleTradingResponse
import ir.devtrader.investor.data.remote.dto.TradesResponse
import ir.devtrader.investor.util.ApiResult
import ir.devtrader.investor.util.toApiResult
import ir.devtrader.investor.util.toApiResultWithError
import kotlinx.serialization.json.Json

class InvestorRepository(
    private val api: ApiService,
    private val json: Json,
) {
    suspend fun getDashboard(): ApiResult<DashboardResponse> = safeCall { api.getDashboard().toApiResult() }

    suspend fun getPositions(): ApiResult<PositionsResponse> = safeCall { api.getPositions().toApiResult() }

    suspend fun getTrades(): ApiResult<TradesResponse> = safeCall { api.getTrades().toApiResult() }

    suspend fun updateProfile(first: String, last: String, phone: String): ApiResult<ProfileResponse> = safeCall {
        api.updateProfile(ProfileRequest(first, last, phone))
            .toApiResultWithError(json, ProfileResponse.serializer()) { it.message }
    }

    suspend fun getDebtLedger(): ApiResult<DebtLedgerResponse> = safeCall { api.getDebtLedger().toApiResult() }

    suspend fun getNotifications(): ApiResult<NotificationsResponse> =
        safeCall { api.getNotifications().toApiResult() }

    suspend fun markNotificationsSeen(): ApiResult<Unit> = safeCall {
        when (val result = api.markNotificationsSeen().toApiResult()) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Error -> result
        }
    }

    suspend fun bindKey(key: String, secret: String): ApiResult<BindKeyResponse> = safeCall {
        api.bindKey(BindKeyRequest(key, secret))
            .toApiResultWithError(json, BindKeyResponse.serializer()) { it.message }
    }

    suspend fun toggleTradingEnabled(): ApiResult<ToggleTradingResponse> = safeCall {
        api.toggleTradingEnabled().toApiResultWithError(json, ToggleTradingResponse.serializer()) { it.message }
    }

    suspend fun updateMarginRatio(marginRatio: Double): ApiResult<MarginRatioResponse> = safeCall {
        api.updateMarginRatio(MarginRatioRequest(marginRatio))
            .toApiResultWithError(json, MarginRatioResponse.serializer()) { it.message }
    }

    suspend fun getAlarms(): ApiResult<AlarmsResponse> = safeCall { api.getAlarms().toApiResult() }

    suspend fun createAlarm(
        symbol: String,
        condition: String,
        price: Double,
        sms: Boolean,
        call: Boolean,
    ): ApiResult<CreateAlarmResponse> = safeCall {
        api.createAlarm(CreateAlarmRequest(symbol, condition, price, sms, call))
            .toApiResultWithError(json, CreateAlarmResponse.serializer()) { it.message }
    }

    suspend fun deleteAlarm(id: String): ApiResult<Unit> = safeCall {
        when (val result = api.deleteAlarm(id).toApiResult()) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Error -> result
        }
    }

    suspend fun getSymbols(): ApiResult<SymbolsResponse> = safeCall { api.getSymbols().toApiResult() }

    private suspend inline fun <T> safeCall(block: suspend () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Network error, please try again")
    }
}
