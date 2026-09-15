package ir.devtrader.investor.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Per-app language, backed by AppCompatDelegate.setApplicationLocales (AndroidX per-app
 * language API) rather than hand-rolled Configuration/Resources overriding — it persists the
 * choice across restarts on its own and, on Android 13+, shows up in the system's own
 * Settings -> Apps -> App language screen for free.
 *
 * MainActivity is a plain ComponentActivity, not AppCompatActivity, so the "no recreate() call
 * needed" guarantee that ships with appcompat 1.6.0+ doesn't apply here (that auto-recreate
 * hooks into AppCompatActivity's own lifecycle) — callers must recreate the activity themselves
 * after calling [setLanguage].
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
