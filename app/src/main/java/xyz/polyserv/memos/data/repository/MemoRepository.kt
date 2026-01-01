package xyz.polyserv.memos.data.repository

import kotlinx.coroutines.flow.Flow
import xyz.polyserv.memos.data.local.LocalDataSource
import xyz.polyserv.memos.data.remote.RemoteDataSource
import xyz.polyserv.memos.data.model.Memo
import xyz.polyserv.memos.data.model.SyncQueueItem
import xyz.polyserv.memos.data.model.SyncAction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoRepository @Inject constructor(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource
) {

    fun getMemos(): Flow<List<Memo>> = localDataSource.getAllMemosFlow()

    suspend fun addMemo(memo: Memo) {
        localDataSource.saveMemo(memo)
        localDataSource.addToSyncQueue(
            SyncQueueItem(
                memoId = memo.id,
                action = SyncAction.CREATE,
                payload = memo.content,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateMemo(memo: Memo) {
        localDataSource.saveMemo(memo)
        localDataSource.addToSyncQueue(
            SyncQueueItem(
                memoId = memo.id,
                action = SyncAction.UPDATE,
                payload = memo.content,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMemo(memoId: String) {
        localDataSource.deleteMemo(memoId)
        localDataSource.addToSyncQueue(
            SyncQueueItem(
                memoId = memoId,
                action = SyncAction.DELETE,
                payload = "",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun searchMemos(query: String): Flow<List<Memo>> =  localDataSource.searchMemos(query)

    suspend fun syncWithServer() {
        try {
            // First get memos from server
            val remoteMemos = remoteDataSource.getAllMemos()

            // Saving locally (update or create)
            for (remoteMemo in remoteMemos) {
                localDataSource.saveMemo(remoteMemo)
            }
            // Sync local changes
            val syncQueue = localDataSource.getSyncQueue()
            for (queueItem in syncQueue) {
                try {
                    when (queueItem.action) {
                        SyncAction.CREATE -> {
                            val memo = localDataSource.getMemoById(queueItem.memoId)
                            if (memo != null) {
                                remoteDataSource.createMemo(memo.content)
                            }
                        }
                        SyncAction.UPDATE -> {
                            val memo = localDataSource.getMemoById(queueItem.memoId)
                            if (memo != null) {
                                remoteDataSource.updateMemo(memo.serverId, memo.content)
                            }
                        }
                        SyncAction.DELETE -> {
                            remoteDataSource.deleteMemo(queueItem.memoId)
                        }
                    }
                    localDataSource.removeSyncQueueItem(queueItem.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
