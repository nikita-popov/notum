package xyz.polyserv.memos.data.remote

import javax.inject.Inject
import timber.log.Timber
import xyz.polyserv.memos.data.model.Memo
import xyz.polyserv.memos.data.model.MemoRequest
import xyz.polyserv.memos.data.model.SyncStatus
import xyz.polyserv.memos.data.model.toMemo

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
                id = response.name,
                content = response.content,
                createTime = response.createTime,
                updateTime = response.updateTime,
                name = response.name,
                //rowStatus = response.rowStatus, TODO
                syncStatus = SyncStatus.SYNCED,
                lastSyncTime = System.currentTimeMillis(),
                isLocalOnly = false,
                serverId = response.name
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create memo. Content: ${content.take(50)}")
            throw Exception("Failed to create memo: ${e.message}", e)
        }
    }

    suspend fun updateMemo(serverId: String, content: String): Memo {
        return try {
            Timber.d("Updating memo: $serverId with content: ${content.take(50)}...")

            val response = apiService.updateMemo(serverId, MemoRequest(content = content))
            Timber.d("Update response received: $response")

            if (response.name.isEmpty()) {
                Timber.e("Response.name is empty for update. Full response: $response")
                throw Exception("API returned empty name in response for update")
            }

            Timber.d("Successfully updated memo: ${response.name}")

            Memo(
                id = response.name,
                content = response.content,
                createTime = response.createTime,
                updateTime = response.updateTime,
                name = response.name,
                //rowStatus = response.rowStatus, TODO
                syncStatus = SyncStatus.SYNCED,
                lastSyncTime = System.currentTimeMillis(),
                isLocalOnly = false,
                serverId = response.name
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to update memo: $serverId")
            throw Exception("Failed to update memo: ${e.message}", e)
        }
    }

    suspend fun deleteMemo(serverId: String) {
        return try {
            Timber.d("Deleting memo: $serverId")
            apiService.deleteMemo(serverId)
            Timber.d("Successfully deleted memo: $serverId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete memo: $serverId")
            throw e
        }
    }
}
