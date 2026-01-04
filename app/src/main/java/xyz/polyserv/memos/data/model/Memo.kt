package xyz.polyserv.memos.data.model

import android.annotation.SuppressLint
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

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
    val id: String = UUID.randomUUID().toString(),

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
    val serverId: String = ""
) {
    override fun toString(): String {
        return "Memo(id=$id, serverId=$serverId, content=${content.take(30)}, syncStatus=$syncStatus, isLocalOnly=$isLocalOnly)"
    }

    /**
     * Получить timestamp создания для сравнения
     */
    fun getCreateTimestamp(): Long {
        return xyz.polyserv.memos.utils.TimeUtils.isoToTimestamp(createTime)
    }

    /**
     * Получить timestamp обновления для сравнения
     */
    fun getUpdateTimestamp(): Long {
        return xyz.polyserv.memos.utils.TimeUtils.isoToTimestamp(updateTime)
    }

    /**
     * Получить отформатированное время создания
     */
    fun getFormattedCreateTime(): String {
        return xyz.polyserv.memos.utils.TimeUtils.formatRelativeTime(createTime)
    }

    /**
     * Получить отформатированное время обновления
     */
    fun getFormattedUpdateTime(): String {
        return xyz.polyserv.memos.utils.TimeUtils.formatRelativeTime(updateTime)
    }

    /**
     * Получить полное отформатированное время создания
     */
    fun getFullCreateTime(): String {
        return xyz.polyserv.memos.utils.TimeUtils.formatFullTime(createTime)
    }

    /**
     * Получить полное отформатированное время обновления
     */
    fun getFullUpdateTime(): String {
        return xyz.polyserv.memos.utils.TimeUtils.formatFullTime(updateTime)
    }

}
