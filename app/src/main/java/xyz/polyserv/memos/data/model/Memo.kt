package xyz.polyserv.memos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

enum class SyncStatus {
    SYNCED, PENDING, SYNCING, FAILED
}

@Entity(tableName = "memos")
@Serializable
data class Memo(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val content: String,
    val resourceName: String = "",
    val createdTs: Long = System.currentTimeMillis(),
    val updatedTs: Long = System.currentTimeMillis(),
    val rowStatus: String = "NORMAL", // NORMAL, ARCHIVED

    // Offline fields
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val lastSyncTime: Long = 0,
    val isLocalOnly: Boolean = true,
    val serverId: String = ""
) {
    override fun toString(): String {
        return "Memo(id=$id, serverId=$serverId, content=${content.take(30)}, syncStatus=$syncStatus, isLocalOnly=$isLocalOnly)"
    }
}

@Serializable
data class MemoRequest(
    val content: String,

    @SerialName("resource_name")
    val resourceName: String = ""
)

@Serializable
data class MemoResponse(
    // Данные приходят на корневом уровне
    val name: String = "",
    val uid: String = "",
    val content: String = "",
    @SerialName("resource_name")
    val resourceName: String = "",
    @SerialName("created_ts")
    val createdTs: Long = 0,
    @SerialName("updated_ts")
    val updatedTs: Long = 0,
    @SerialName("row_status")
    val rowStatus: String = "NORMAL",
    val state: String = "NORMAL",
    val creator: String = "",
    @SerialName("createTime")
    val createTime: String = "",
    @SerialName("updateTime")
    val updateTime: String = "",
    @SerialName("displayTime")
    val displayTime: String = "",
    val visibility: String = "PRIVATE",
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val attachments: List<String> = emptyList(),
    val relations: List<String> = emptyList(),
    val reactions: List<String> = emptyList(),
    val property: MemoProperty? = null,
    val snippet: String = ""
) {
    override fun toString(): String {
        return "MemoResponse(name=$name, content=${content.take(30)})"
    }
}

@Serializable
data class MemoProperty(
    val hasLink: Boolean = false,
    val hasTaskList: Boolean = false,
    val hasCode: Boolean = false,
    val hasIncompleteTasks: Boolean = false
)

@Serializable
data class MemoData(
    val name: String = "",
    val uid: String = "",
    val content: String = "",
    @SerialName("resource_name")
    val resourceName: String = "",
    @SerialName("created_ts")
    val createdTs: Long = 0,
    @SerialName("updated_ts")
    val updatedTs: Long = 0,
    @SerialName("row_status")
    val rowStatus: String = "NORMAL"
) {
    override fun toString(): String {
        return "MemoData(name=$name, content=${content.take(30)}, uid=$uid)"
    }
}

@Serializable
data class ListMemosResponse(
    val memos: List<MemoData> = emptyList()
) {
    override fun toString(): String {
        return "ListMemosResponse(count=${memos.size})"
    }
}
