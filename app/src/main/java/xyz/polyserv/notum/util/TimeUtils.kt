package xyz.polyserv.notum.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

object TimeUtils {

    /**
     * Конвертирует ISO 8601 строку в timestamp (миллисекунды)
     */
    fun isoToTimestamp(isoString: String): Long {
        return if (isoString.isEmpty()) {
            0L
        } else {
            try {
                Instant.parse(isoString).toEpochMilli()
            } catch (e: Exception) {
                0L
            }
        }
    }

    /**
     * Конвертирует timestamp в ISO 8601 строку (UTC)
     */
    fun timestampToIso(timestamp: Long): String {
        return if (timestamp == 0L) {
            ""
        } else {
            Instant.ofEpochMilli(timestamp).toString()
        }
    }

    /**
     * Возвращает текущее время в формате ISO 8601 (UTC)
     */
    fun getCurrentTimeIso(): String {
        return Instant.now().toString()
    }

    /**
     * Возвращает текущий timestamp в миллисекундах
     */
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }

    /**
     * Форматирует ISO 8601 время для отображения пользователю
     * Примеры: "2 часа назад", "Вчера", "15 янв 2024"
     */
    fun formatRelativeTime(isoString: String): String {
        if (isoString.isEmpty()) return ""

        return try {
            val instant = Instant.parse(isoString)
            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            val now = LocalDateTime.now()

            val minutesAgo = ChronoUnit.MINUTES.between(dateTime, now)
            val hoursAgo = ChronoUnit.HOURS.between(dateTime, now)
            val daysAgo = ChronoUnit.DAYS.between(dateTime, now)

            when {
                minutesAgo < 1 -> "Только что"
                minutesAgo < 60 -> "$minutesAgo мин назад"
                hoursAgo < 24 -> "$hoursAgo ч назад"
                daysAgo == 0L -> "Сегодня"
                daysAgo == 1L -> "Вчера"
                daysAgo < 7 -> "$daysAgo дн назад"
                else -> {
                    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
                    dateTime.format(formatter)
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Форматирует ISO 8601 время в полный формат (локальное время)
     * Пример: "15 января 2024, 14:30"
     */
    fun formatFullTime(isoString: String): String {
        if (isoString.isEmpty()) return ""

        return try {
            val instant = Instant.parse(isoString)
            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.getDefault())
            dateTime.format(formatter)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Сравнивает два ISO 8601 времени
     * Возвращает: отрицательное число если time1 < time2,
     *            0 если равны,
     *            положительное число если time1 > time2
     */
    fun compareIsoTimes(time1: String, time2: String): Int {
        val timestamp1 = isoToTimestamp(time1)
        val timestamp2 = isoToTimestamp(time2)
        return timestamp1.compareTo(timestamp2)
    }
}
