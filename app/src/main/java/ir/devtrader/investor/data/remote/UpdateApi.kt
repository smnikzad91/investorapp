package ir.devtrader.investor.data.remote

import ir.devtrader.investor.data.remote.dto.CheckUpdateResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** Public, unauthenticated endpoint — served from BuildConfig.UPDATE_BASE_URL, not BASE_URL. */
interface UpdateApi {
    @GET("api/v1/app/check-update")
    suspend fun checkUpdate(@Query("version_code") versionCode: Int): CheckUpdateResponse
}
