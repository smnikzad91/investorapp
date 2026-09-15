package ir.devtrader.investor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import ir.devtrader.investor.navigation.TradeBotNavGraph
import ir.devtrader.investor.ui.theme.TradeBotInvestorTheme
import ir.devtrader.investor.ui.update.UpdateGate
import ir.devtrader.investor.ui.update.UpdateUiState
import ir.devtrader.investor.ui.update.UpdateViewModel

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate() per the core-splashscreen contract.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val appContainer = (application as TradeBotApplication).appContainer

        // Created here (not inside UpdateGate/Compose) so the splash's keepOnScreenCondition can
        // read its state directly before setContent even runs; the same instance is then handed
        // into UpdateGate below instead of it creating its own.
        val updateViewModel = ViewModelProvider(
            this,
            UpdateViewModel.factory(appContainer.updateApi, appContainer.apkDownloader, appContainer.updateCheckCache),
        )[UpdateViewModel::class.java]

        // Keeps the splash up only for the update-check network round trip itself, not for
        // however long a flexible/immediate dialog stays on screen afterward — once resolved,
        // the splash hands off to the normal Compose UI with UpdateGate's dialog already
        // overlaid on top of it if there's an update to show.
        splashScreen.setKeepOnScreenCondition { updateViewModel.uiState.value is UpdateUiState.Checking }

        setContent {
            TradeBotInvestorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    UpdateGate(viewModel = updateViewModel) {
                        TradeBotNavGraph(appContainer = appContainer)
                    }
                }
            }
        }
    }
}
