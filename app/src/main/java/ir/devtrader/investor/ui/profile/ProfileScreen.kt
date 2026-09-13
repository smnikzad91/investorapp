package ir.devtrader.investor.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.devtrader.investor.data.repository.InvestorRepository
import ir.devtrader.investor.ui.common.Banner
import ir.devtrader.investor.ui.common.FullScreenError
import ir.devtrader.investor.ui.common.FullScreenLoading

@Composable
fun ProfileScreen(investorRepository: InvestorRepository, modifier: Modifier = Modifier) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(investorRepository))
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> FullScreenLoading()
        uiState.error != null -> FullScreenError(uiState.error!!, onRetry = viewModel::refresh)
        else -> Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            OutlinedTextField(
                value = uiState.email,
                onValueChange = {},
                readOnly = true,
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )
            OutlinedTextField(
                value = uiState.first,
                onValueChange = viewModel::onFirstChange,
                label = { Text("First name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )
            OutlinedTextField(
                value = uiState.last,
                onValueChange = viewModel::onLastChange,
                label = { Text("Last name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            )
            OutlinedTextField(
                value = uiState.phone,
                onValueChange = viewModel::onPhoneChange,
                label = { Text("Phone") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "This is the number SMS/call price alarms use.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            uiState.resultMessage?.let {
                Banner(message = it, isError = uiState.isResultError)
            }

            Button(
                onClick = viewModel::save,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Save")
                }
            }
        }
    }
}
