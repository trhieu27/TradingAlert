package com.tradingalert.receiver.localization

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

internal enum class AppLanguage(val languageTag: String) {
    VIETNAMESE("vi"),
    ENGLISH("en"),
}

internal object AppLanguageManager {
    private const val PREFERENCES_NAME = "app_language"
    private const val KEY_LANGUAGE_TAG = "language_tag"

    fun savedLanguage(context: Context): AppLanguage {
        val savedTag = context
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE_TAG, null)

        return AppLanguage.entries.firstOrNull { it.languageTag == savedTag }
            ?: AppLanguage.VIETNAMESE
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_TAG, language.languageTag)
            .apply()
    }

    fun localizedContext(
        context: Context,
        language: AppLanguage = savedLanguage(context),
    ): Context {
        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList(Locale.forLanguageTag(language.languageTag)))
        }
        return context.createConfigurationContext(configuration)
    }
}
