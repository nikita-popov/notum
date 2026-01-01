package xyz.polyserv.memos.data.local.database

import androidx.room.TypeConverter
import xyz.polyserv.memos.data.model.SyncStatus

enum class SyncAction(val value: String) {
    CREATE("CREATE"),
    UPDATE("UPDATE"),
    DELETE("DELETE")
}

class Converters {
    @TypeConverter
    fun fromSyncStatus(value: SyncStatus): String = value.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = SyncStatus.valueOf(value)

    @TypeConverter
    fun toSyncAction(value: String): SyncAction {
        return SyncAction.values().first { it.value == value }
    }

    @TypeConverter
    fun fromSyncAction(action: SyncAction): String {
        return action.value
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toTimestamp(value: String?): Long? {
        return value?.toLongOrNull()
    }
}
