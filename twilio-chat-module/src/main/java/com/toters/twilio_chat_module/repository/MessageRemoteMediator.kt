package com.toters.twilio_chat_module.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.HttpException
import com.toters.twilio_chat_module.ConversationsClientWrapper
import com.toters.twilio_chat_module.extensions.*
import com.toters.twilio_chat_module.localCache.LocalCacheProvider
import com.toters.twilio_chat_module.localCache.entity.MessageDataItem
import com.toters.twilio_chat_module.models.RepositoryRequestStatus
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val channelSid: String,
    private val pageSize: Int,
    private val fetchStatusFlow: MutableStateFlow<RepositoryRequestStatus>,
    private val database: LocalCacheProvider,
    private val networkService: ConversationsClientWrapper
) : RemoteMediator<Int, MessageDataItem>() {
    private val messagesDao = database.messagesDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MessageDataItem>,
    ): MediatorResult {
        return try {
            // The network load method takes an optional after=<user.id>
            // parameter. For every page after the first, pass the last user
            // ID to let it continue from where it left off. For REFRESH,
            // pass null to load the first page.
            val firstItem = state.firstItemOrNull()
            val loadKey = when (loadType) {
                LoadType.REFRESH -> null
                // In this example, you never need to prepend, since REFRESH
                // will always load the first page in the list. Immediately
                // return, reporting end of pagination.
                LoadType.PREPEND -> {
                    firstItem ?: return MediatorResult.Success(
                        endOfPaginationReached = true
                    )

                    firstItem.index
                }
                LoadType.APPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

            val identity = networkService.getConversationsClient().myIdentity
            fetchStatusFlow.value = RepositoryRequestStatus.FETCHING
            val messages = if (loadKey == null) {
                networkService.getConversationsClient().getConversation(channelSid)
                    .getLastMessages(pageSize)
            } else {
                networkService.getConversationsClient().getConversation(channelSid)
                    .getMessagesBefore(loadKey - 1, pageSize)
            }

            database.withTransaction {
                messagesDao.insert(messages.asMessageDataItems(identity))
                fetchStatusFlow.value = RepositoryRequestStatus.COMPLETE
            }

            MediatorResult.Success(
                endOfPaginationReached = messages[0].messageIndex == 0.toLong()
            )
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        } catch (e: ConversationsException) {
            MediatorResult.Error(e)
        }
    }
}