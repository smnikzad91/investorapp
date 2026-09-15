package ir.devtrader.investor.ui.update

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ir.devtrader.investor.BuildConfig
import ir.devtrader.investor.data.remote.UpdateApi
import ir.devtrader.investor.data.remote.dto.CheckUpdateResponse
import ir.devtrader.investor.update.ApkDownloader
import ir.devtrader.investor.update.UpdateCheckCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UpdateUiState {
    /** Initial state: the check is still in flight. MainActivity keeps the splash up for this. */
    data object Checking : UpdateUiState

    /** Resolved: nothing to show — either no update was available, or a flexible one was dismissed. */
    data object NoUpdate : UpdateUiState

    data class Flexible(val info: CheckUpdateResponse, val downloading: Boolean = false, val failed: Boolean = false) :
        UpdateUiState
    data class Immediate(val info: CheckUpdateResponse, val downloading: Boolean = false, val failed: Boolean = false) :
        UpdateUiState
}

/**
 * Checks for an update once per app process (via [checkCache], not just once per ViewModel —
 * MainActivity.recreate() on a language switch would otherwise spin up a fresh instance of this
 * ViewModel and redo the network round trip / re-show the splash every time), comparing
 * BuildConfig.VERSION_CODE against the backend's latestVersionCode.
 */
class UpdateViewModel(
    private val updateApi: UpdateApi,
    private val apkDownloader: ApkDownloader,
    private val checkCache: UpdateCheckCache,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Checking)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    init {
        checkForUpdate()
    }

    private fun checkForUpdate() {
        if (checkCache.hasChecked) {
            applyResult(checkCache.result)
            return
        }
        viewModelScope.launch {
            val result = try {
                updateApi.checkUpdate(BuildConfig.VERSION_CODE)
            } catch (e: Exception) {
                // Network/malformed-response failure must never block app launch — fail open.
                // It'll retry on the next cold start.
                Log.w(TAG, "check-update failed, continuing without blocking", e)
                checkCache.store(null)
                applyResult(null)
                return@launch
            }
            checkCache.store(result)
            applyResult(result)
        }
    }

    private fun applyResult(result: CheckUpdateResponse?) {
        if (result == null || !result.updateAvailable || result.downloadUrl == null) {
            _uiState.value = UpdateUiState.NoUpdate
            return
        }
        _uiState.value = if (result.isMandatory) {
            UpdateUiState.Immediate(result)
        } else {
            UpdateUiState.Flexible(result)
        }
    }

    /** No-op unless a flexible dialog is showing — the mandatory dialog has no "later" path. */
    fun onLaterClicked() {
        if (_uiState.value is UpdateUiState.Flexible) {
            _uiState.value = UpdateUiState.NoUpdate
        }
    }

    fun onUpdateNowClicked() {
        val info = when (val state = _uiState.value) {
            is UpdateUiState.Flexible -> state.info
            is UpdateUiState.Immediate -> state.info
            UpdateUiState.Checking, UpdateUiState.NoUpdate -> return
        }
        val downloadUrl = info.downloadUrl ?: return

        setDialogState(info, downloading = true, failed = false)
        apkDownloader.download(
            downloadUrl = downloadUrl,
            versionName = info.latestVersionName ?: "latest",
            onComplete = { apkFile ->
                apkDownloader.promptInstall(apkFile)
                // The dialog is left up (behind the installer): if the user backs out without
                // completing the install, they land back on it rather than into the app.
                setDialogState(info, downloading = false, failed = false)
            },
            onFailed = { setDialogState(info, downloading = false, failed = true) },
        )
    }

    private fun setDialogState(info: CheckUpdateResponse, downloading: Boolean, failed: Boolean) {
        _uiState.value = if (info.isMandatory) {
            UpdateUiState.Immediate(info, downloading, failed)
        } else {
            UpdateUiState.Flexible(info, downloading, failed)
        }
    }

    companion object {
        private const val TAG = "UpdateViewModel"

        fun factory(
            updateApi: UpdateApi,
            apkDownloader: ApkDownloader,
            checkCache: UpdateCheckCache,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { UpdateViewModel(updateApi, apkDownloader, checkCache) }
        }
    }
}
