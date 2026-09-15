package ir.devtrader.investor.ui.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Wraps the app content with the update dialog, if any. [viewModel] is created once in
 * MainActivity (not here) so the same instance can also drive the splash screen's
 * keepOnScreenCondition — see MainActivity.onCreate.
 *
 * Content is always composed underneath — the mandatory dialog blocks interaction with it (a
 * modal Dialog window swallows touches to what's behind it and ignores back), not by hiding
 * the nav graph.
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
