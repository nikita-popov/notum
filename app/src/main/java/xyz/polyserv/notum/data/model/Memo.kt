package xyz.polyserv.notum.data.model

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable
import xyz.polyserv.notum.util.TimeUtils

enum class SyncStatus {
    SYNCED, PENDING, SYNCING, FAILED
}

enum class Visibility {
    PRIVATE, PROTECTED, PUBLIC
}

enum class MemoState {
    NORMAL, ARCHIVED
}

@SuppressLint("UnsafeOptInUsageError")
@Entity(tableName = "memos")
@Serializable
data class Memo(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(), // Local ID

    // Main
    val content: String,
    val name: String = "", // Resource name from server (memos/{id})
    val uid: String = "", // Server UID
    val creator: String = "", // Format: users/{user}

    // Time
    val createTime: String = "", // ISO 8601 timestamp
    val updateTime: String = "", // ISO 8601 timestamp
    val displayTime: String = "", // ISO 8601 timestamp

    // State and visibility
    val state: MemoState = MemoState.NORMAL,
    val visibility: Visibility = Visibility.PRIVATE,
    val pinned: Boolean = false,

    // Offline fields
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncTime: Long = 0,
    val isLocalOnly: Boolean = true,
) {
    override fun toString(): String {
        return "Memo(id=$id, name=$name, content=${content.take(30)}, syncStatus=$syncStatus, isLocalOnly=$isLocalOnly)"
    }

    fun getCreateTimestamp(): Long {
        return TimeUtils.isoToTimestamp(createTime)
    }

    fun getUpdateTimestamp(): Long {
        return TimeUtils.isoToTimestamp(updateTime)
    }

    fun getFormattedCreateTime(context: Context): String {
        return TimeUtils.formatRelativeTime(context, createTime)
    }

    fun getFormattedUpdateTime(context: Context): String {
        return TimeUtils.formatRelativeTime(context,updateTime)
    }

    fun getFullCreateTime(): String {
        return TimeUtils.formatFullTime(createTime)
    }

    fun getFullUpdateTime(): String {
        return TimeUtils.formatFullTime(updateTime)
    }
}
