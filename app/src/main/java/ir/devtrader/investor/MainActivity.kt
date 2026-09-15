package ir.devtrader.investor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import ir.devtrader.investor.navigation.TradeBotNavGraph
import ir.devtrader.investor.ui.theme.TradeBotInvestorTheme
import ir.devtrader.investor.ui.update.UpdateGate
import ir.devtrader.investor.ui.update.UpdateViewModel

/**
 * AppCompatActivity (not plain ComponentActivity) is required here — on API < 33,
 * AppCompatDelegate.setApplicationLocales() only actually changes what locale an Activity's
 * resources use via AppCompatActivity's attachBaseContext() hook. Without it, LanguageManager's
 * locale choice gets recorded but the UI itself never re-renders in the new language. See
 * res/values/themes.xml — Theme.TradeBotInvestor had to become an AppCompat-descended theme too.
 */
class MainActivity : AppCompatActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate() per the core-splashscreen contract. Only ever covers
        // the very first frame — it auto-dismisses as soon as Compose draws anything (Login, or
        // Dashboard with its own skeleton — see UpdateGate/DashboardSkeleton). The update check
        // itself runs silently in the background and only ever surfaces as a dialog if there's
        // actually an update.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val appContainer = (application as TradeBotApplication).appContainer

        val updateViewModel = ViewModelProvider(
            this,
            UpdateViewModel.factory(appContainer.updateApi, appContainer.apkDownloader, appContainer.updateCheckCache),
        )[UpdateViewModel::class.java]

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
