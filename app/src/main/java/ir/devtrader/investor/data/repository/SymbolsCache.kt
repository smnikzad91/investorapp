package ir.devtrader.investor.data.repository

import ir.devtrader.investor.util.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-scoped, not screen-scoped: GET /symbols is fetched once (from AppContainer, while the app
 * is starting up / right after login) and kept in memory here, so opening the Alarms screen's
 * symbol dropdown never waits on a network round trip — it just reads whatever's already cached.
 */
class SymbolsCache(private val investorRepository: InvestorRepository) {

    private val _symbols = MutableStateFlow<List<String>>(emptyList())
    val symbols: StateFlow<List<String>> = _symbols.asStateFlow()

    suspend fun refresh() {
        val result = investorRepository.getSymbols()
        if (result is ApiResult.Success) {
            _symbols.value = result.data.symbols
        }
    }
}
