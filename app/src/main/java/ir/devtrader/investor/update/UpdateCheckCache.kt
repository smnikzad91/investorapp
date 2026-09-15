package ir.devtrader.investor.update

import ir.devtrader.investor.data.remote.dto.CheckUpdateResponse

/**
 * Process-scoped (lives in AppContainer, not the ViewModel) cache of the one-time check-update
 * result. Without this, MainActivity.recreate() — which the Settings language picker calls after
 * every switch — would spin up a brand new UpdateViewModel and re-hit the network, and briefly
 * re-show the splash screen, on every single language change.
 */
class UpdateCheckCache {
    @Volatile var hasChecked: Boolean = false
        private set

    @Volatile var result: CheckUpdateResponse? = null
        private set

    fun store(result: CheckUpdateResponse?) {
        this.result = result
        hasChecked = true
    }
}
