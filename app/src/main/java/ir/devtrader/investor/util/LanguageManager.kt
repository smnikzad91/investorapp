package ir.devtrader.investor.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Per-app language, backed by AppCompatDelegate.setApplicationLocales (AndroidX per-app
 * language API) rather than hand-rolled Configuration/Resources overriding — it persists the
 * choice across restarts on its own and, on Android 13+, shows up in the system's own
 * Settings -> Apps -> App language screen for free.
 *
 * MainActivity extends AppCompatActivity specifically so this works on API < 33 — the automatic
 * locale-application (and appcompat 1.6.0+'s own recreate()-on-locale-change) both hook into
 * AppCompatActivity's attachBaseContext()/lifecycle; a plain ComponentActivity never picks up the
 * new locale there. Settings' language picker still calls recreate() itself too, as a fallback.
 */
object LanguageManager {
    const val ENGLISH = "en"
    const val PERSIAN = "fa"
    val SUPPORTED = listOf(ENGLISH, PERSIAN)

    fun setLanguage(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }

    fun currentLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) ENGLISH else locales[0]?.language ?: ENGLISH
    }
}
