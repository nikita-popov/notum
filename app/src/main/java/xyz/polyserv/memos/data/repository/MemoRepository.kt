package xyz.polyserv.memos.data.repository

import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import xyz.polyserv.memos.data.local.LocalDataSource
import xyz.polyserv.memos.data.remote.RemoteDataSource
import xyz.polyserv.memos.data.model.Memo
import xyz.polyserv.memos.data.model.SyncQueueItem
import xyz.polyserv.memos.data.model.SyncAction
import xyz.polyserv.memos.data.model.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton

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
        localDataSource.saveMemo(memo)
        localDataSource.addToSyncQueue(
            SyncQueueItem(
                memoId = memo.id,
                action = SyncAction.CREATE,
                payload = memo.content,
                timestamp = System.currentTimeMillis()
            )
        )
        Timber.d("Memo added to sync queue: ${memo.id}")
    }

    suspend fun updateMemo(memo: Memo) {
        Timber.d("Updating memo: $memo")
        localDataSource.saveMemo(memo)
        localDataSource.addToSyncQueue(
            SyncQueueItem(
                memoId = memo.id,
                action = SyncAction.UPDATE,
                payload = memo.content,
                timestamp = System.currentTimeMillis()
            )
        )
        Timber.d("Memo update added to sync queue: ${memo.id}")
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
            // First sync local memos to server
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
                                Timber.d("CREATE action - Deleting old local memo: ${memo.id}")
                                localDataSource.deleteMemo(memo.id)

                                Timber.d("CREATE action - Saving server memo: ${serverMemo.id}")
                                val syncedMemo = serverMemo.copy(
                                    syncStatus = SyncStatus.SYNCED,
                                    lastSyncTime = System.currentTimeMillis(),
                                    isLocalOnly = false
                                )
                                localDataSource.saveMemo(syncedMemo)

                                // Remove memo from queue
                                localDataSource.removeSyncQueueItem(queueItem.id)

                                Timber.d("Memo created and synced: ${memo.id} -> ${serverMemo.id}")
                            } else {
                                // Memo was removed locally, remove it from queue
                                Timber.w("CREATE action - Memo not found: ${queueItem.memoId}")
                                localDataSource.removeSyncQueueItem(queueItem.id)
                            }
                        }

                        SyncAction.UPDATE -> {
                            if (memo != null && memo.serverId.isNotEmpty()) {
                                Timber.d("UPDATE action - Setting SYNCING status")
                                val syncingMemo = memo.copy(syncStatus = SyncStatus.SYNCING)
                                localDataSource.saveMemo(syncingMemo)

                                Timber.d("UPDATE action - Calling remote API for serverId: ${memo.serverId}")
                                val serverMemo = remoteDataSource.updateMemo(memo.serverId, memo.content)

                                Timber.d("UPDATE action - Server returned: $serverMemo")

                                Timber.d("UPDATE action - Updating local memo")
                                val syncedMemo = serverMemo.copy(
                                    syncStatus = SyncStatus.SYNCED,
                                    lastSyncTime = System.currentTimeMillis()
                                )
                                localDataSource.saveMemo(syncedMemo)

                                // Remove memo from queue
                                localDataSource.removeSyncQueueItem(queueItem.id)

                                Timber.d("Memo updated and synced: ${memo.id}")
                            } else {
                                // Memo still not synced or removed
                                Timber.w("UPDATE action - Memo not found or no serverId: ${queueItem.memoId}")
                                localDataSource.removeSyncQueueItem(queueItem.id)
                            }
                        }

                        SyncAction.DELETE -> {
                            // Try to find by serverId for removing
                            val deletedMemo = memo
                            if (deletedMemo?.serverId?.isNotEmpty() == true) {
                                remoteDataSource.deleteMemo(deletedMemo.serverId)
                            }
                            // Remove from queue in any case
                            localDataSource.removeSyncQueueItem(queueItem.id)
                            Timber.d("Memo deleted on server: ${queueItem.memoId}")
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
                    val existingMemo = localDataSource.getMemoById(remoteMemo.id)

                    if (existingMemo == null) {
                        Timber.d("New memo from server: ${remoteMemo.id}")
                        val syncedMemo = remoteMemo.copy(
                            syncStatus = SyncStatus.SYNCED,
                            lastSyncTime = System.currentTimeMillis(),
                            isLocalOnly = false
                        )
                        localDataSource.saveMemo(syncedMemo)
                    } else {
                        Timber.d("Updating memo from server: ${remoteMemo.id} (server: ${remoteMemo.updatedTs}, local: ${existingMemo.updatedTs})")
                        if (remoteMemo.updatedTs > existingMemo.updatedTs) {
                            val updatedMemo = remoteMemo.copy(
                                syncStatus = SyncStatus.SYNCED,
                                lastSyncTime = System.currentTimeMillis()
                            )
                            localDataSource.saveMemo(updatedMemo)
                            Timber.d("Memo updated from server: ${remoteMemo.id}")
                        } else {
                            val syncedMemo = remoteMemo.copy(
                                syncStatus = SyncStatus.SYNCED,
                                lastSyncTime = System.currentTimeMillis(),
                                isLocalOnly = false
                            )
                            localDataSource.saveMemo(syncedMemo)
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
