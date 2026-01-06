package xyz.polyserv.notum.data.model

import android.os.Build
import androidx.work.impl.model.systemIdInfo
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String) {
    SYSTEM("system", "System"),
    ENGLISH("en", "English"),
    RUSSIAN("ru", "Русский");

    fun toLocale(): Locale? {
        return when (this) {
            SYSTEM -> null
            else -> when (Build.VERSION.SDK_INT) {
                in 1..35 -> Locale(code)
                else -> Locale.of(code)
            }
        }
    }

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return AppLanguage.entries.find { it.code == code } ?: SYSTEM
        }
    }
}
