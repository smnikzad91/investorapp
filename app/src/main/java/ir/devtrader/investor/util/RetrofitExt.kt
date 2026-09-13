package ir.devtrader.investor.util

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import retrofit2.Response

/** For endpoints whose failure responses don't carry a useful parseable message. */
fun <T> Response<T>.toApiResult(): ApiResult<T> {
    if (isSuccessful) {
        return body()?.let { ApiResult.Success(it) }
            ?: ApiResult.Error("Empty response from server")
    }
    return ApiResult.Error("Request failed (${code()})")
}

/** For endpoints (login, bind-key) whose failure body is JSON worth showing to the user verbatim. */
fun <T, E> Response<T>.toApiResultWithError(
    json: Json,
    errorSerializer: DeserializationStrategy<E>,
    message: (E) -> String?,
): ApiResult<T> {
    if (isSuccessful) {
        return body()?.let { ApiResult.Success(it) }
            ?: ApiResult.Error("Empty response from server")
    }
    val raw = errorBody()?.string()
    val parsedMessage = raw?.let {
        runCatching { json.decodeFromString(errorSerializer, it) }.getOrNull()?.let(message)
    }
    return ApiResult.Error(parsedMessage ?: "Request failed (${code()})")
}
