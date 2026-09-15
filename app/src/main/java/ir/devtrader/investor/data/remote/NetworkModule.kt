package ir.devtrader.investor.data.remote

import ir.devtrader.investor.BuildConfig
import ir.devtrader.investor.data.local.SessionManager
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun buildApiService(sessionManager: SessionManager): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor(sessionManager))
            .addInterceptor(UnauthorizedInterceptor(sessionManager))
            // BASIC only: request/response bodies can carry the auth token or the investor's
            // exchange API key/secret and must never hit logcat.
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(ApiService::class.java)
    }

    /** No auth interceptor: check-update is public and called before any session may exist. */
    fun buildUpdateApi(): UpdateApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.UPDATE_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(UpdateApi::class.java)
    }
}

/** Attaches `Authorization: Bearer <token>` to every request except login, which has none yet. */
private class AuthHeaderInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.url.encodedPath.endsWith("/auth/login")) {
            return chain.proceed(original)
        }
        val token = sessionManager.token() ?: return chain.proceed(original)
        val authenticated = original.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(authenticated)
    }
}

/** There is no refresh-token endpoint: any 401 means "log the user out and show Login again". */
private class UnauthorizedInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 401 && !request.url.encodedPath.endsWith("/auth/login")) {
            sessionManager.clearSession()
        }
        return response
    }
}
