package com.toters.twilio_chat_module.repository

import androidx.annotation.WorkerThread
import androidx.paging.*
import com.toters.twilio_chat_module.ConversationsClientWrapper
import com.toters.twilio_chat_module.extensions.*
import com.toters.twilio_chat_module.models.RepositoryRequestStatus
import com.toters.twilio_chat_module.models.RepositoryResult
import com.twilio.conversations.Conversation
import com.twilio.conversations.Message
import com.twilio.conversations.Participant.Type.CHAT
import com.twilio.conversations.User
import com.toters.twilio_chat_module.localCache.LocalCacheProvider
import com.toters.twilio_chat_module.localCache.entity.ConversationDataItem
import com.toters.twilio_chat_module.localCache.entity.MessageDataItem
import com.toters.twilio_chat_module.localCache.entity.ParticipantDataItem
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import timber.log.Timber

interface ConversationsRepository {
    fun getUserConversations(): Flow<RepositoryResult<List<ConversationDataItem>>>
    fun getConversation(conversationSid: String): Flow<RepositoryResult<ConversationDataItem?>>
    fun getMyIdentity(): Flow<String>
    fun getSelfUser(): Flow<User>
    fun getMessageByUuid(messageUuid: String): MessageDataItem?
    // Interim solution till paging v3.0 is available as an alpha version.
    // It has support for converting PagedList types
    fun getMessages(conversationSid: String, pageSize: Int): Flow<RepositoryResult<PagingData<MessageDataItem>>>
    fun getUnSentMessages(conversationSid: String): Flow<RepositoryResult<List<MessageDataItem>>>
    fun getSendingMessages(): List<MessageDataItem>
    fun insertMessage(message: MessageDataItem)
    fun updateMessageByUuid(message: MessageDataItem)
    fun updateMessageStatus(messageUuid: String, sendStatus: Int, errorCode: Int)
    fun getTypingParticipants(conversationSid: String): Flow<List<ParticipantDataItem>>
    fun getConversationParticipants(conversationSid: String): Flow<RepositoryResult<List<ParticipantDataItem>>>
    fun updateMessageMediaDownloadStatus(
        messageSid: String,
        downloadId: Long? = null,
        downloadLocation: String? = null,
        downloadState: Int? = null,
        downloadedBytes: Long? = null
    )
    fun updateMessageMediaUploadStatus(
        messageUuid: String,
        uploading: Boolean? = null,
        uploadedBytes: Long? = null
    )
    fun clear()
    fun subscribeToConversationsClientEvents()
    fun unsubscribeFromConversationsClientEvents()
}

class ConversationsRepositoryImpl(
    private val conversationsClientWrapper: ConversationsClientWrapper,
    private val localCache: LocalCacheProvider
) : ConversationsRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val clientListener = createClientListener(
            onConversationDeleted = { conversation ->
                launch {
                    Timber.d("Conversation deleted $conversation")
                    localCache.conversationsDao().delete(conversation.sid)
                }
            },
            onConversationUpdated = { conversation, _ ->
                launch{ insertOrUpdateConversation(conversation.sid) }
            },
            onConversationAdded = { conversation ->
                launch {
                    insertOrUpdateConversation(conversation.sid)
                }
            },
            onConversationSynchronizationChange = { conversation ->
                launch { insertOrUpdateConversation(conversation.sid) }
            }
    )

    private val conversationListener = createConversationListener(
        onTypingStarted = { conversation, participant ->
            Timber.d("${participant.identity} started typing in ${conversation.friendlyName}")
            this@ConversationsRepositoryImpl.launch {
                val user = participant.getAndSubscribeUser()
                localCache.participantsDao().insertOrReplace(participant.asParticipantDataItem(typing = true, user))
            }
        },
        onTypingEnded = { conversation, participant ->
            Timber.d("${participant.identity} stopped typing in ${conversation.friendlyName}")
            this@ConversationsRepositoryImpl.launch {
                val user = participant.getAndSubscribeUser()
                localCache.participantsDao().insertOrReplace(participant.asParticipantDataItem(typing = false, user))
            }
        },
        onParticipantAdded = { participant ->
            Timber.d("${participant.identity} added in ${participant.conversation.sid}")
            this@ConversationsRepositoryImpl.launch {
                val user = participant.getAndSubscribeUser()
                localCache.participantsDao().insertOrReplace(participant.asParticipantDataItem(user = user))
            }
        },
        onParticipantUpdated = { participant, reason ->
            Timber.d("${participant.identity} updated in ${participant.conversation.sid}, reason: $reason")
            this@ConversationsRepositoryImpl.launch {
                val user = participant.getAndSubscribeUser()
                localCache.participantsDao().insertOrReplace(participant.asParticipantDataItem(user = user))
            }
        },
        onParticipantDeleted = { participant ->
            Timber.d("${participant.identity} deleted in ${participant.conversation.sid}")
            this@ConversationsRepositoryImpl.launch {
                localCache.participantsDao().delete(participant.asParticipantDataItem())
            }
        },
        onMessageDeleted = { message ->
            deleteMessage(message)
        },
        onMessageUpdated = { message, reason ->
            updateMessage(message, reason)
        },
        onMessageAdded = { message ->
            addMessage(message)
        }
    )

    private fun launch(block: suspend CoroutineScope.() -> Unit) = repositoryScope.launch(
        context = CoroutineExceptionHandler { _, e -> Timber.e(e, "Coroutine failed ${e.localizedMessage}") },
        block = block
    )

    override fun getUserConversations(): Flow<RepositoryResult<List<ConversationDataItem>>> {
        val localDataFlow = localCache.conversationsDao().getUserConversations()
        val fetchStatusFlow = fetchConversations().flowOn(Dispatchers.IO)

        return combine(localDataFlow, fetchStatusFlow) { data, status -> RepositoryResult(data, status) }
    }

    override fun getConversation(conversationSid: String): Flow<RepositoryResult<ConversationDataItem?>> {
        val localDataFlow = localCache.conversationsDao().getConversation(conversationSid)
        val fetchStatusFlow = fetchConversation(conversationSid).flowOn(Dispatchers.IO)

        return combine(localDataFlow, fetchStatusFlow) { data, status ->
            RepositoryResult(data, status)
        }
    }

    override fun getMessageByUuid(messageUuid: String) = localCache.messagesDao().getMessageByUuid(messageUuid)

    @OptIn(ExperimentalPagingApi::class)
    override fun getMessages(conversationSid: String, pageSize: Int): Flow<RepositoryResult<PagingData<MessageDataItem>>> {
        Timber.v("getMessages($conversationSid, $pageSize)")

        val fetchStatusFlow = MutableStateFlow<RepositoryRequestStatus>(RepositoryRequestStatus.NONE)

        val pager = Pager(
            config = PagingConfig(pageSize = pageSize),
            remoteMediator = MessageRemoteMediator(
                conversationSid,
                pageSize,
                fetchStatusFlow,
                localCache,
                conversationsClientWrapper
            )
        ) {
            localCache.messagesDao().getMessagesSorted(conversationSid)
        }

        return combine(pager.flow.cachedIn(repositoryScope), fetchStatusFlow) { data, status ->
            RepositoryResult(data, status)
        }
    }

    @WorkerThread
    override fun getUnSentMessages(conversationSid: String): Flow<RepositoryResult<List<MessageDataItem>>> {
        val localDataFlow = localCache.messagesDao().getUnSentMessages(conversationSid)
        val fetchStatusFlow = fetchConversation(conversationSid).flowOn(Dispatchers.IO)

        return combine(localDataFlow, fetchStatusFlow) { data, status ->
            RepositoryResult(data, status)
        }
    }

    override fun getSendingMessages(): List<MessageDataItem> = localCache.messagesDao().getSendingMessages()

    override fun insertMessage(message: MessageDataItem) {
        launch {
            localCache.messagesDao().insertOrReplace(message)
            updateConversationLastMessage(message.conversationSid)
        }
    }

    override fun updateMessageByUuid(message: MessageDataItem) {
        launch {
            localCache.messagesDao().updateByUuidOrInsert(message)
            updateConversationLastMessage(message.conversationSid)
        }
    }

    override fun updateMessageStatus(messageUuid: String, sendStatus: Int, errorCode: Int) {
        launch {
            localCache.messagesDao().updateMessageStatus(messageUuid, sendStatus, errorCode)

            val message = localCache.messagesDao().getMessageByUuid(messageUuid) ?: return@launch
            updateConversationLastMessage(message.conversationSid)
        }
    }

    override fun getTypingParticipants(conversationSid: String): Flow<List<ParticipantDataItem>> =
        localCache.participantsDao().getTypingParticipants(conversationSid)

    override fun getConversationParticipants(conversationSid: String): Flow<RepositoryResult<List<ParticipantDataItem>>> {
        val localDataFlow = localCache.participantsDao().getAllParticipants(conversationSid)
        val fetchStatusFlow = fetchParticipants(conversationSid).flowOn(Dispatchers.IO)

        return combine(localDataFlow, fetchStatusFlow) { data, status -> RepositoryResult(data, status) }
    }

    override fun updateMessageMediaDownloadStatus(
        messageSid: String,
        downloadId: Long?,
        downloadLocation: String?,
        downloadState: Int?,
        downloadedBytes: Long?
    ) {
        launch {
            if (downloadId != null) {
                localCache.messagesDao().updateMediaDownloadId(messageSid, downloadId)
            }
            if (downloadLocation != null) {
                localCache.messagesDao().updateMediaDownloadLocation(messageSid, downloadLocation)
            }
            if (downloadState != null) {
                localCache.messagesDao().updateMediaDownloadState(messageSid, downloadState)
            }
            if (downloadedBytes != null) {
                localCache.messagesDao().updateMediaDownloadedBytes(messageSid, downloadedBytes)
            }
        }
    }

    override fun updateMessageMediaUploadStatus(
        messageUuid: String,
        uploading: Boolean?,
        uploadedBytes: Long?
    ) {
        launch {
            if (uploading != null) {
                localCache.messagesDao().updateMediaUploadStatus(messageUuid, uploading)
            }
            if (uploadedBytes != null) {
                localCache.messagesDao().updateMediaUploadedBytes(messageUuid, uploadedBytes)
            }
        }
    }


    override fun clear() {
        launch {
            localCache.clearAllTables()
        }
    }

    override fun getMyIdentity(): Flow<String> = flow {
        emit(conversationsClientWrapper.getConversationsClient().myIdentity)
    }

    override fun getSelfUser(): Flow<User> = callbackFlow {
        val client = conversationsClientWrapper.getConversationsClient()
        val listener = createClientListener (
            onUserUpdated = { user, _ ->
                user.takeIf { it.identity == client.myIdentity}
                    ?.let { trySend(it).isSuccess }
            }
        )
        client.addListener(listener)
        send(client.myUser)
        awaitClose { client.removeListener(listener) }
    }

    private fun fetchMessages(conversationSid: String, fetch: suspend Conversation.() -> List<Message>) = flow {
        emit(RepositoryRequestStatus.FETCHING)
        try {
            val identity = conversationsClientWrapper.getConversationsClient().myIdentity
            val messages = conversationsClientWrapper
                .getConversationsClient()
                .getConversation(conversationSid)
                .waitForSynchronization()
                .fetch()
                .asMessageDataItems(identity)
            localCache.messagesDao().insert(messages)
            if (messages.isNotEmpty()) {
                updateConversationLastMessage(conversationSid)
            }
            emit(RepositoryRequestStatus.COMPLETE)
        } catch (e: ConversationsException) {
            Timber.d("fetchMessages error: ${e.error.message}")
            emit(RepositoryRequestStatus.Error(e.error))
        }
    }

    private fun fetchConversation(conversationSid: String) = flow {
        emit(RepositoryRequestStatus.FETCHING)
        try {
            insertOrUpdateConversation(conversationSid)
            emit(RepositoryRequestStatus.COMPLETE)
        } catch (e: ConversationsException) {
            Timber.d("fetchConversations error: ${e.error.message}")
            emit(RepositoryRequestStatus.Error(e.error))
        }
    }

    private fun fetchParticipants(conversationSid: String) = flow {
        emit(RepositoryRequestStatus.FETCHING)
        try {
            val conversation = conversationsClientWrapper.getConversationsClient().getConversation(conversationSid)
            conversation.waitForSynchronization()
            conversation.participantsList.forEach { participant ->
                // Getting user is currently supported for chat participants only
                val user = if (participant.type == CHAT) participant.getAndSubscribeUser() else null
                localCache.participantsDao().insertOrReplace(participant.asParticipantDataItem(user = user))
            }
            emit(RepositoryRequestStatus.COMPLETE)
        } catch (e: ConversationsException) {
            Timber.d("fetchParticipants error: ${e.error.message}")
            emit(RepositoryRequestStatus.Error(e.error))
        }
    }

    private fun fetchConversations() = channelFlow {
        send(RepositoryRequestStatus.FETCHING)

        try {
            // get items from client
            val dataItems = conversationsClientWrapper
                    .getConversationsClient()
                    .myConversations
                    .map { it.toConversationDataItem() }
            Timber.d("repo dataItems from client $dataItems")

            localCache.conversationsDao().deleteGoneUserConversations(dataItems)
            send(RepositoryRequestStatus.SUBSCRIBING)

            var status: RepositoryRequestStatus = RepositoryRequestStatus.COMPLETE
            supervisorScope {
                // get all conversations and update conversation data in local cache
                dataItems.forEach {
                    launch {
                        try {
                            insertOrUpdateConversation(it.sid)
                        } catch (e: ConversationsException) {
                            Timber.d("insertOrUpdateConversation error: ${e.error.message}")
                            status = RepositoryRequestStatus.Error(e.error)
                        }
                    }
                }
            }
            Timber.d("fetchConversations completed with status: $status")
            send(status)
        } catch (e: ConversationsException) {
            Timber.d("fetchConversations error: ${e.error.message}")
            send(RepositoryRequestStatus.Error(e.error))
        }
    }

    override fun subscribeToConversationsClientEvents() {
        launch {
            Timber.d("Client listener added")
            conversationsClientWrapper.getConversationsClient().addListener(clientListener)
        }
    }

    override fun unsubscribeFromConversationsClientEvents() {
        launch {
            Timber.d("Client listener removed")
            conversationsClientWrapper.getConversationsClient().removeListener(clientListener)
        }
    }

    private suspend fun insertOrUpdateConversation(conversationSid: String) {
        val conversation = conversationsClientWrapper.getConversationsClient().getConversation(conversationSid)
        Timber.d("repo updating dataItem in db... ${conversation.friendlyName}")
        conversation.addListener(conversationListener)
        localCache.conversationsDao().insert(conversation.toConversationDataItem())
        localCache.conversationsDao().update(conversation.sid,
            conversation.status.value, conversation.notificationLevel.value, conversation.friendlyName)
        launch {
            localCache.conversationsDao().updateParticipantCount(conversationSid, conversation.getParticipantCount())
        }
        launch {
            localCache.conversationsDao().updateMessagesCount(conversationSid, conversation.getMessageCount())
        }
        launch {
            localCache.conversationsDao().updateUnreadMessagesCount(conversationSid, conversation.getUnreadMessageCount() ?: return@launch)
        }
        launch {
            updateConversationLastMessage(conversationSid)
        }
    }

    private suspend fun updateConversationLastMessage(conversationSid: String) {
        val lastMessage = localCache.messagesDao().getLastMessage(conversationSid)
        if (lastMessage != null) {
            localCache.conversationsDao().updateLastMessage(
                conversationSid, lastMessage.body ?: "", lastMessage.sendStatus, lastMessage.dateCreated)
        } else {
            fetchMessages(conversationSid) { getLastMessages(10) }.collect()
        }
    }

    private fun deleteMessage(message: Message) {
        launch {
            val identity = conversationsClientWrapper.getConversationsClient().myIdentity
            Timber.d("Message deleted: ${message.toMessageDataItem(identity)}")
            localCache.messagesDao().delete(message.toMessageDataItem(identity))
            updateConversationLastMessage(message.conversationSid)
        }
    }

    private fun updateMessage(message: Message, updateReason: Message.UpdateReason? = null) {
        launch {
            val identity = conversationsClientWrapper.getConversationsClient().myIdentity
            val uuid = localCache.messagesDao().getMessageBySid(message.sid)?.uuid ?: ""
            Timber.d("Message updated: ${message.toMessageDataItem(identity)}, reason: $updateReason")
            localCache.messagesDao().insertOrReplace(message.toMessageDataItem(identity, uuid))
            updateConversationLastMessage(message.conversationSid)
        }
    }

    private fun addMessage(message: Message) {
        launch {
            val identity = conversationsClientWrapper.getConversationsClient().myIdentity
            Timber.d("Message added: ${message.toMessageDataItem(identity)}")
            localCache.messagesDao().updateByUuidOrInsert(message.toMessageDataItem(identity, message.getUUID()))
            updateConversationLastMessage(message.conversationSid)
        }
    }

    companion object {
        val INSTANCE get() = _instance ?: error("call ConversationsRepository.createInstance() first")

        private var _instance: ConversationsRepository? = null

        fun createInstance(conversationsClientWrapper: ConversationsClientWrapper, localCache: LocalCacheProvider) {
            check(_instance == null) { "ConversationsRepository singleton instance has been already created" }
            _instance = ConversationsRepositoryImpl(conversationsClientWrapper, localCache)
        }
    }
}
