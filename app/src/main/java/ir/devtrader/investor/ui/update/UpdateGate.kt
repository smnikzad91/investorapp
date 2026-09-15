package ir.devtrader.investor.ui.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Wraps the app content with the update dialog, if any. [viewModel] is created once in
 * MainActivity (not here) so both this and MainActivity read the exact same instance/state.
 *
 * [content] (the real nav graph — Login or Dashboard, depending on session state) always renders,
 * including while the update check is still in flight (UpdateUiState.Checking) — there's no
 * separate splash/loading screen gating it: Login has nothing to wait on, and Dashboard shows its
 * own skeleton (DashboardSkeleton) while its first load is in progress. The mandatory dialog blocks
 * interaction with the content behind it (a modal Dialog window swallows touches and ignores
 * back), not by hiding the nav graph.
 */
@Composable
fun UpdateGate(viewModel: UpdateViewModel, content: @Composable () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    content()

    when (val state = uiState) {
        is UpdateUiState.Immediate -> MandatoryUpdateDialog(state, onUpdateNow = viewModel::onUpdateNowClicked)
        is UpdateUiState.Flexible -> OptionalUpdateDialog(
            state = state,
            onUpdateNow = viewModel::onUpdateNowClicked,
            onLater = viewModel::onLaterClicked,
        )
        UpdateUiState.Checking, UpdateUiState.NoUpdate -> Unit
    }
}
