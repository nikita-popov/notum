package xyz.polyserv.notum.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import xyz.polyserv.notum.data.local.LocalDataSource
import xyz.polyserv.notum.data.remote.RemoteDataSource
import xyz.polyserv.notum.data.model.Memo
import xyz.polyserv.notum.data.model.SyncQueueItem
import xyz.polyserv.notum.data.model.SyncAction
import xyz.polyserv.notum.data.model.SyncStatus
import xyz.polyserv.notum.utils.TimeUtils

@Singleton
class MemoRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource
) {

    fun getMemos(): Flow<List<Memo>> = localDataSource.getAllMemosFlow()

    suspend fun getMemoById(memoId: String): Memo? {
        return localDataSource.getMemoById(memoId)
    }

    suspend fun addMemo(memo: Memo) {
        Timber.d("Adding memo: $memo")
        // Устанавливаем UTC время при создании
        val currentTimeUtc = TimeUtils.getCurrentTimeIso()
        val memoWithUtcTime = memo.copy(
            createTime = currentTimeUtc,
            updateTime = currentTimeUtc
        )
        localDataSource.saveMemo(memoWithUtcTime)
        localDataSource.addToSyncQueue(
            SyncQueueItem(
                memoId = memoWithUtcTime.id,
                action = SyncAction.CREATE,
                payload = memoWithUtcTime.content,
                timestamp = System.currentTimeMillis()
            )
        )
        Timber.d("Memo added to sync queue: ${memoWithUtcTime.id}")
    }

    suspend fun updateMemo(memo: Memo) {
        Timber.d("Updating memo: $memo")
        // Устанавливаем UTC время при обновлении
        val currentTimeUtc = TimeUtils.getCurrentTimeIso()
        val memoWithUtcTime = memo.copy(
            updateTime = currentTimeUtc
        )
        localDataSource.saveMemo(memoWithUtcTime)
        val action = if (memoWithUtcTime.name.isNotEmpty() || !memoWithUtcTime.isLocalOnly) {
            SyncAction.UPDATE
        } else {
            SyncAction.CREATE
        }
        localDataSource.addToSyncQueue(
            SyncQueueItem(
                memoId = memoWithUtcTime.id,
                action = action,
                payload = memoWithUtcTime.content,
                timestamp = System.currentTimeMillis()
            )
        )
        Timber.d("Memo update added to sync queue: ${memoWithUtcTime.id}")
    }

    suspend fun deleteMemo(memoId: String) {
        Timber.d("Deleting memo: $memoId")
        localDataSource.deleteMemo(memoId)
        localDataSource.addToSyncQueue(
            SyncQueueItem(
                memoId = memoId,
                action = SyncAction.DELETE,
                payload = "",
                timestamp = System.currentTimeMillis()
            )
        )
        Timber.d("Memo delete added to sync queue: $memoId")
    }

    fun searchMemos(query: String): Flow<List<Memo>> = localDataSource.searchMemos(query)

    suspend fun syncWithServer() {
        Timber.d("========== SYNC START ==========")
        try {
            // First sync local changes to server
            val syncQueue = localDataSource.getSyncQueue()
            Timber.d("Sync queue size: ${syncQueue.size}")

            for (queueItem in syncQueue) {
                try {
                    val memo = localDataSource.getMemoById(queueItem.memoId)
                    Timber.d("Processing queue item: action=${queueItem.action}, memoId=${queueItem.memoId}, memo=$memo")

                    when (queueItem.action) {
                        SyncAction.CREATE -> {
                            if (memo != null) {
                                Timber.d("CREATE action - Setting SYNCING status")
                                val syncingMemo = memo.copy(syncStatus = SyncStatus.SYNCING)
                                localDataSource.saveMemo(syncingMemo)

                                Timber.d("CREATE action - Calling remote API")
                                val serverMemo = remoteDataSource.createMemo(memo.content)

                                Timber.d("CREATE action - Server returned: $serverMemo")

                                Timber.d("CREATE action - Updating memo with serverId: ${serverMemo.name}")
                                val syncedMemo = memo.copy(
                                    name = serverMemo.name,
                                    createTime = serverMemo.createTime,
                                    updateTime = serverMemo.updateTime,
                                    syncStatus = SyncStatus.SYNCED,
                                    lastSyncTime = System.currentTimeMillis(),
                                    isLocalOnly = false
                                )
                                localDataSource.saveMemo(syncedMemo)

                                // Remove memo from queue
                                localDataSource.removeSyncQueueItem(queueItem.id)

                                Timber.d("CREATE action - Memo created and synced: ${memo.id} -> ${serverMemo.id}")
                            } else {
                                // Memo was removed locally, remove it from queue
                                Timber.w("CREATE action - Memo not found: ${queueItem.memoId}")
                                localDataSource.removeSyncQueueItem(queueItem.id)
                            }
                        }

                        SyncAction.UPDATE -> {
                            if (memo != null && memo.name.isNotEmpty()) {
                                Timber.d("UPDATE action - Setting SYNCING status")
                                val syncingMemo = memo.copy(syncStatus = SyncStatus.SYNCING)
                                localDataSource.saveMemo(syncingMemo)

                                Timber.d("UPDATE action - Calling remote API for serverId: ${memo.name}")
                                val serverMemo = remoteDataSource.updateMemo(memo.name, memo.content)

                                Timber.d("UPDATE action - Server returned: $serverMemo")

                                Timber.d("UPDATE action - Updating local memo with server update time")
                                val syncedMemo = memo.copy(
                                    updateTime = serverMemo.updateTime,
                                    syncStatus = SyncStatus.SYNCED,
                                    lastSyncTime = System.currentTimeMillis()
                                )
                                localDataSource.saveMemo(syncedMemo)

                                // Remove memo from queue
                                localDataSource.removeSyncQueueItem(queueItem.id)

                                Timber.d("UPDATE action - Memo updated and synced: ${memo.id}")
                            } else {
                                if (memo != null && memo.name.isEmpty()) {
                                    Timber.w("UPDATE action - No serverId yet, checking if CREATE is pending")
                                    val hasPendingCreate = syncQueue.any {
                                        it.memoId == memo.id && it.action == SyncAction.CREATE && it.id < queueItem.id
                                    }
                                    if (hasPendingCreate) {
                                        Timber.d("UPDATE action - CREATE is pending, keeping UPDATE in queue")
                                        // Don't remove from queue, process after CREATE
                                    } else {
                                        Timber.w("UPDATE action - No pending CREATE, removing UPDATE from queue")
                                        localDataSource.removeSyncQueueItem(queueItem.id)
                                    }
                                } else {
                                    Timber.w("UPDATE action - Memo not found or no serverId: ${queueItem.memoId}")
                                    localDataSource.removeSyncQueueItem(queueItem.id)
                                }
                            }
                        }

                        SyncAction.DELETE -> {
                            // Try to find by serverId for removing
                            val deletedMemo = memo
                            if (deletedMemo?.name?.isNotEmpty() == true) {
                                remoteDataSource.deleteMemo(deletedMemo.name)
                            }
                            // Remove from queue in any case
                            localDataSource.removeSyncQueueItem(queueItem.id)
                            Timber.d("DELETE action - Memo deleted on server: ${queueItem.memoId}")
                        }
                    }
                } catch (e: Exception) {
                    val memo = localDataSource.getMemoById(queueItem.memoId)
                    if (memo != null) {
                        val failedMemo = memo.copy(syncStatus = SyncStatus.FAILED)
                        localDataSource.saveMemo(failedMemo)
                        Timber.e(e, "Failed to sync memo: ${memo.id} (action=${queueItem.action})")
                    } else {
                        Timber.e(e, "Failed to sync memo: ${queueItem.memoId} - memo not found (action=${queueItem.action})")
                    }
                }
            }

            // Second, get all memos from server
            try {
                Timber.d("Fetching all memos from server")
                val remoteMemos = remoteDataSource.getAllMemos()
                Timber.d("Received ${remoteMemos.size} memos from server")

                for (remoteMemo in remoteMemos) {
                    // Check if memo exists locally
                    val existingMemo = localDataSource.getMemoByName(remoteMemo.name)
                    if (existingMemo == null) {
                        Timber.d("New memo from server: ${remoteMemo.id}")
                        val syncedMemo = remoteMemo.copy(
                            syncStatus = SyncStatus.SYNCED,
                            lastSyncTime = System.currentTimeMillis(),
                            isLocalOnly = false,
                            name = remoteMemo.id
                        )
                        localDataSource.saveMemo(syncedMemo)
                    } else {
                        val remoteTimestamp = remoteMemo.getUpdateTimestamp()
                        val localTimestamp = existingMemo.getUpdateTimestamp()

                        Timber.d("Comparing timestamps for ${remoteMemo.id}: remote=$remoteTimestamp (${remoteMemo.updateTime}), local=$localTimestamp (${existingMemo.updateTime})")

                        if (remoteTimestamp > localTimestamp) {
                            Timber.d("Updating memo from server: ${remoteMemo.id}")
                            val updatedMemo = existingMemo.copy(
                                content = remoteMemo.content,
                                updateTime = remoteMemo.updateTime,
                                createTime = remoteMemo.createTime,
                                syncStatus = SyncStatus.SYNCED,
                                lastSyncTime = System.currentTimeMillis()
                            )
                            localDataSource.saveMemo(updatedMemo)
                            Timber.d("Memo updated from server: ${remoteMemo.id}")
                        } else {
                            Timber.d("Memo up-to-date: ${remoteMemo.id}")
                        }
                    }
                }
                Timber.d("Server fetch completed")
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch memos from server")
                throw e
            }

            Timber.d("========== SYNC SUCCESS ==========")
        } catch (e: Exception) {
            Timber.e(e, "========== SYNC FAILED ==========")
            throw e
        }
    }
}
