package xyz.polyserv.notum.data.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MemoRequest(
    val content: String,
    val visibility: String = "PRIVATE",
    val pinned: Boolean = false,
    @SerialName("display_time")
    val displayTime: String? = null,
    @SerialName("update_time")
    val updateTime: String? = null,
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MemoResponse(
    // ID's
    val name: String = "", // Format: memos/{memo}
    val uid: String = "",
    val creator: String = "", // Format: users/{user}

    // Time
    val createTime: String = "",
    val updateTime: String = "",
    val displayTime: String = "",

    // Content
    val content: String = "",
    val snippet: String = "",

    // Status
    val state: String = "NORMAL",
    val visibility: String = "PRIVATE",
    val pinned: Boolean = false,

    // Metadata
    val tags: List<String> = emptyList(),
    val property: MemoPropertyResponse? = null,

    // Relations
    val attachments: List<AttachmentResponse> = emptyList(),
    val relations: List<MemoRelationResponse> = emptyList(),
    val reactions: List<ReactionResponse> = emptyList(),

    // Additional
    val parent: String? = null, // Format: memos/{memo}
    val location: LocationResponse? = null
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class AttachmentResponse(
    val name: String = "",
    val uid: String = "",
    val type: String = "",
    val size: Long = 0,
    val filename: String = "",
    val memo: String = ""
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MemoRelationResponse(
    val memo: MemoRelationMemo? = null,
    @SerialName("related_memo")
    val relatedMemo: MemoRelationMemo? = null,
    val type: String = "REFERENCE"
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MemoRelationMemo(
    val name: String = "",
    val snippet: String = ""
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ReactionResponse(
    val name: String = "",
    val creator: String = "",
    @SerialName("content_id")
    val contentId: String = "",
    @SerialName("reaction_type")
    val reactionType: String = "",
    @SerialName("create_time")
    val createTime: String = ""
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MemoPropertyResponse(
    @SerialName("has_link")
    val hasLink: Boolean = false,
    @SerialName("has_task_list")
    val hasTaskList: Boolean = false,
    @SerialName("has_code")
    val hasCode: Boolean = false,
    @SerialName("has_incomplete_tasks")
    val hasIncompleteTasks: Boolean = false
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class LocationResponse(
    val placeholder: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class ListMemosResponse(
    val memos: List<MemoResponse> = emptyList(),
    @SerialName("next_page_token")
    val nextPageToken: String = ""
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UpdateMemoRequest(
    val memo: MemoRequest,
    @SerialName("update_mask")
    val updateMask: String
)

// Mapping
fun MemoResponse.toMemo(): Memo {
    return Memo(
        id = uid.ifEmpty { name.substringAfterLast("/") },
        content = content,
        name = name,
        uid = uid,
        creator = creator,
        createTime = createTime,
        updateTime = updateTime,
        displayTime = displayTime.ifEmpty { createTime },
        state = when (state) {
            "ARCHIVED" -> MemoState.ARCHIVED
            else -> MemoState.NORMAL
        },
        visibility = when (visibility) {
            "PUBLIC" -> Visibility.PUBLIC
            "PROTECTED" -> Visibility.PROTECTED
            else -> Visibility.PRIVATE
        },
        pinned = pinned,
        //snippet = snippet,
        //tags = tags,
        //attachments = attachments.map { it.toAttachment() },
        //relations = relations.map { it.toMemoRelation() },
        //reactions = reactions.map { it.toReaction() },
        //property = property?.toMemoProperty(),
        //location = location?.toLocation(),
        //parent = parent,
        syncStatus = SyncStatus.SYNCED,
        lastSyncTime = System.currentTimeMillis(),
        isLocalOnly = false,
    )
}

/*fun AttachmentResponse.toAttachment() = Attachment(
    name = name,
    uid = uid,
    type = type,
    size = size,
    filename = filename,
    memo = memo
)

fun MemoRelationResponse.toMemoRelation() = MemoRelation(
    memoId = memo?.name ?: "",
    relatedMemoId = relatedMemo?.name ?: "",
    type = when (type) {
        "COMMENT" -> RelationType.COMMENT
        else -> RelationType.REFERENCE
    },
    snippet = relatedMemo?.snippet ?: ""
)

fun ReactionResponse.toReaction() = Reaction(
    name = name,
    creator = creator,
    contentId = contentId,
    reactionType = reactionType,
    createTime = createTime
)*/

fun MemoPropertyResponse.toMemoProperty() = MemoProperty(
    hasLink = hasLink,
    hasTaskList = hasTaskList,
    hasCode = hasCode,
    hasIncompleteTasks = hasIncompleteTasks
)

/*fun LocationResponse.toLocation() = Location(
    placeholder = placeholder,
    latitude = latitude,
    longitude = longitude
)*/

fun Memo.toMemoRequest() = MemoRequest(
    content = content,
    visibility = visibility.name,
    pinned = pinned,
    displayTime = displayTime.ifEmpty { null },
    //location = location
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class MemoProperty(
    val hasLink: Boolean = false,
    val hasTaskList: Boolean = false,
    val hasCode: Boolean = false,
    val hasIncompleteTasks: Boolean = false
)
