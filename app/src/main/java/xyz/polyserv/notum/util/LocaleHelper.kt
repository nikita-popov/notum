package xyz.polyserv.notum.util

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import java.util.Locale
import xyz.polyserv.notum.data.model.AppLanguage

object LocaleHelper {

    fun setLocale(activity: Activity, language: AppLanguage) {
        val locale = language.toLocale() ?: getSystemLocale()
        Locale.setDefault(locale)

        val config = Configuration(activity.resources.configuration)
        config.setLocale(locale)

        // Используем современный подход для API 17+
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val context = activity.createConfigurationContext(config)
            activity.resources.updateConfiguration(
                context.resources.configuration,
                context.resources.displayMetrics
            )
        } else {
            @Suppress("DEPRECATION")
            activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
        }

        // Пересоздаём активность для применения изменений
        activity.recreate()
        */
    }

    private fun getSystemLocale(): Locale {
        return Locale.getDefault(Locale.Category.DISPLAY)
    }

    fun getDisplayName(language: AppLanguage, currentLocale: Locale): String {
        return when (language) {
            AppLanguage.SYSTEM -> "System"
            else -> language.displayName
        }
    }
}
