package xyz.polyserv.notum.data.remote

import javax.inject.Inject
import timber.log.Timber
import xyz.polyserv.notum.data.model.Memo
import xyz.polyserv.notum.data.model.MemoRequest
import xyz.polyserv.notum.data.model.SyncStatus
import xyz.polyserv.notum.data.model.UpdateMemoRequest
import xyz.polyserv.notum.data.model.toMemo
import xyz.polyserv.notum.util.TimeUtils

class RemoteDataSource @Inject constructor(
    private val apiService: MemosApiService
) {
    suspend fun getAllMemos(): List<Memo> {
        return try {
            val response = apiService.listMemos()
            Timber.d("getAllMemos response: ${response.memos.size} memos")
            response.memos.map { it.toMemo() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch all memos")
            throw e
        }
    }

    suspend fun createMemo(content: String): Memo {
        return try {
            Timber.d("Creating memo with content: ${content.take(50)}...")

            val response = apiService.createMemo(MemoRequest(content = content))
            Timber.d("Create response received: $response")

            if (response.name.isEmpty()) {
                Timber.e("Response.name is empty. Full response: $response")
                throw Exception("API returned empty name in response. Response: $response")
            }

            Timber.d("Successfully created memo: ${response.name}")

            Memo(
                name = response.name,
                content = response.content,
                createTime = response.createTime,
                updateTime = response.updateTime,
                //rowStatus = response.rowStatus, TODO
                syncStatus = SyncStatus.SYNCED,
                lastSyncTime = System.currentTimeMillis(),
                isLocalOnly = false
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create memo. Content: ${content.take(50)}")
            throw Exception("Failed to create memo: ${e.message}", e)
        }
    }

    suspend fun updateMemo(name: String, content: String): Memo {
        return try {
            Timber.d("Updating memo: $name with content: ${content.take(50)}...")

            val currentTimeUtc = TimeUtils.getCurrentTimeIso()
            val response = apiService.updateMemo(
                name = name.stripMemosPrefix(),
                request = MemoRequest(
                    content = content,
                    updateTime = currentTimeUtc
                ),
                updateMask = "content,update_time"
            )
            Timber.d("Update response received: $response")

            if (response.name.isEmpty()) {
                Timber.e("Response.name is empty for update. Full response: $response")
                throw Exception("API returned empty name in response for update")
            }

            Timber.d("Successfully updated memo: ${response.name}")

            Memo(
                content = response.content,
                createTime = response.createTime,
                updateTime = response.updateTime,
                name = response.name,
                syncStatus = SyncStatus.SYNCED,
                lastSyncTime = System.currentTimeMillis(),
                isLocalOnly = false,
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to update memo: $name")
            throw Exception("Failed to update memo: ${e.message}", e)
        }
    }

    suspend fun deleteMemo(name: String) {
        return try {
            Timber.d("Deleting memo: $name")
            apiService.deleteMemo(name.stripMemosPrefix())
            Timber.d("Successfully deleted memo: $name")
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete memo: $name")
            throw e
        }
    }

    private fun String.stripMemosPrefix(): String =
        removePrefix("memos/")
}
