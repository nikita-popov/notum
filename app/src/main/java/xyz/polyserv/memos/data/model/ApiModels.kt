package xyz.polyserv.memos.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemoResponseWrapper(
    val data: MemoData? = null,
    val memo: MemoData? = null,
    val message: String? = null,
    val code: Int? = null
)

@Serializable
data class CreateMemoRequest(
    val content: String,
    @SerialName("resource_name")
    val resourceName: String = ""
)

object ApiDebugLogger {
    fun logMemoResponse(response: MemoResponse) {
        if (response.name.isEmpty()) {
            println("MEMO IS NULL IN RESPONSE")
        } else {
            println("Memo Response: id=${response.name}, content=${response.content.take(30)}")
        }
    }
}
