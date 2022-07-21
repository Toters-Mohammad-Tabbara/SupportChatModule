package com.toters.twilio_chat_module.viewModels

import android.app.DownloadManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.paging.PagingData
import androidx.paging.map
import com.toters.twilio_chat_module._common.SingleLiveEvent
import com.toters.twilio_chat_module.enums.ConversationsError
import com.toters.twilio_chat_module.enums.DownloadState
import com.toters.twilio_chat_module.enums.SendStatus
import com.toters.twilio_chat_module.extensions.*
import com.toters.twilio_chat_module.localCache.entity.MessageDataItem
import com.toters.twilio_chat_module.localCache.entity.ParticipantDataItem
import com.toters.twilio_chat_module.manager.MessageListManager
import com.toters.twilio_chat_module.models.MessageListViewItem
import com.toters.twilio_chat_module.models.RepositoryRequestStatus
import com.toters.twilio_chat_module.repository.ConversationsRepository
import com.twilio.messaging.internal.ApplicationContextHolder
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.InputStream
import java.util.*

const val MESSAGE_COUNT = 50

class MessageListViewModel constructor(
    private val applicationContextHolder: ApplicationContextHolder,
    val conversationSid: String,
    private val conversationsRepository: ConversationsRepository,
    private val messageListManager: MessageListManager
) : ViewModel() {

    companion object {
        val listOfResentMessageSids = mutableListOf<String>()
    }

    val conversationName = MutableLiveData<String>()

    val selfUser = conversationsRepository.getSelfUser().asLiveData(viewModelScope.coroutineContext)

    val messageItems = conversationsRepository.getMessages(conversationSid, MESSAGE_COUNT)
        .onEach { repositoryResult ->
            if (repositoryResult.requestStatus is RepositoryRequestStatus.Error) {
                onMessageError.postValue(ConversationsError.CONVERSATION_GET_FAILED)
            }
        }
        .asLiveData(viewModelScope.coroutineContext)
        .map { it.data }

    val unsentMessages = SingleLiveEvent<List<MessageDataItem>?>()

    val openMedia = SingleLiveEvent<Uri>()

    fun getUnsentMessages() {
        viewModelScope.launch {
            conversationsRepository.getUnSentMessages(conversationSid).collect {
                unsentMessages.value = it.data
            }
        }
    }

    val onMessageError = SingleLiveEvent<ConversationsError>()

    val onMessageSent = SingleLiveEvent<String>()

    val onMessageCopied = SingleLiveEvent<Unit>()

//    val selectedMessage: MessageListViewItem? get() = messageItems.value?.firstOrNull { it.index == selectedMessageIndex }

    val typingParticipantsList = conversationsRepository.getTypingParticipants(conversationSid)
        .map { participants -> participants.map { it.typingIndicatorName } }
        .distinctUntilChanged()
        .asLiveData(viewModelScope.coroutineContext)

    private val messagesObserver: Observer<PagingData<MessageDataItem>> =
        Observer { list ->
            list.map { message ->
                if (message.mediaDownloadState == DownloadState.DOWNLOADING.value && message.mediaDownloadId != null) {
                    if (updateMessageMediaDownloadState(message.index, message.mediaDownloadId)) {
                        observeMessageMediaDownload(message.index, message.mediaDownloadId)
                    }
                }
            }
        }

    init {
        Timber.d("init: $conversationSid")
        viewModelScope.launch {
            getConversationResult()
        }
        messageItems.observeForever(messagesObserver)
    }

    override fun onCleared() {
        messageItems.removeObserver(messagesObserver)
    }

    private suspend fun getConversationResult() {
        conversationsRepository.getConversation(conversationSid).collect { result ->
            if (result.requestStatus is RepositoryRequestStatus.Error) {
                onMessageError.value = ConversationsError.CONVERSATION_GET_FAILED
                return@collect
            }
            conversationName.value = result.data?.friendlyName?.takeIf { it.isNotEmpty() } ?: result.data?.sid
        }
    }

    fun sendTextMessage(message: String) = viewModelScope.launch {
        val messageUuid = UUID.randomUUID().toString()
        listOfResentMessageSids.add(messageUuid)
        Timber.d("messageUuid: $messageUuid")
        try {
            messageListManager.sendTextMessage(message, messageUuid)
            onMessageSent.call(messageUuid)
            listOfResentMessageSids.remove(messageUuid)
            Timber.d("Message sent: $messageUuid")
        } catch (e: ConversationsException) {
            Timber.d("Text message send error: ${e.errorInfo?.status}:${e.errorInfo?.code} ${e.errorInfo?.message}")
            messageListManager.updateMessageStatus(messageUuid, SendStatus.ERROR, e.errorInfo?.code ?: 0)
            onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
        }
    }

    fun resendTextMessage(messageUuid: String) = viewModelScope.launch {
        if (!listOfResentMessageSids.contains(messageUuid)) {
            listOfResentMessageSids.add(messageUuid)
            try {
                messageListManager.retrySendTextMessage(messageUuid)
                onMessageSent.call(messageUuid)
                listOfResentMessageSids.remove(messageUuid)
                Timber.d("Message re-sent: $messageUuid")
            } catch (e: ConversationsException) {
                listOfResentMessageSids.remove(messageUuid)
                messageListManager.updateMessageStatus(
                    messageUuid,
                    SendStatus.ERROR,
                    e.errorInfo?.code ?: 0
                )
                onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
            }
        }
    }

    fun sendMediaMessage(uri: String, inputStream: InputStream, fileName: String?, mimeType: String?) =
        viewModelScope.launch {
            val messageUuid = UUID.randomUUID().toString()
            listOfResentMessageSids.add(messageUuid)
            try {
                messageListManager.sendMediaMessage(uri, inputStream, fileName, mimeType, messageUuid)
                onMessageSent.call(messageUuid)
                listOfResentMessageSids.remove(messageUuid)
                Timber.d("Media message sent: $messageUuid")
            } catch (e: ConversationsException) {
                Timber.d("Media message send error: ${e.errorInfo?.status}:${e.errorInfo?.code} ${e.errorInfo?.message}")
                messageListManager.updateMessageStatus(messageUuid, SendStatus.ERROR, e.errorInfo?.code ?: 0)
                onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
            }
        }

    fun resendMediaMessage(inputStream: InputStream, messageUuid: String) = viewModelScope.launch {
        if (!listOfResentMessageSids.contains(messageUuid)) {
            listOfResentMessageSids.add(messageUuid)
            try {
                messageListManager.retrySendMediaMessage(inputStream, messageUuid)
                onMessageSent.call(messageUuid)
                listOfResentMessageSids.remove(messageUuid)
                Timber.d("Media re-sent: $messageUuid")
            } catch (e: ConversationsException) {
                listOfResentMessageSids.remove(messageUuid)
                messageListManager.updateMessageStatus(
                    messageUuid,
                    SendStatus.ERROR,
                    e.errorInfo?.code ?: 0
                )
                onMessageError.value = ConversationsError.MESSAGE_SEND_FAILED
            }
        }
    }

    fun handleMessageDisplayed(messageIndex: Long) = viewModelScope.launch {
        try {
            messageListManager.notifyMessageRead(messageIndex)
        } catch (e: ConversationsException) {
            // Ignored
        } catch (e: IllegalStateException) {

        }
    }

    fun typing() = viewModelScope.launch {
        try {
            Timber.d("Typing in conversation $conversationSid")
            messageListManager.typing()
        } catch (e: ConversationsException) {
            e.printStackTrace()
        }
    }

    private fun updateMessageMediaDownloadStatus(
        messageIndex: Long,
        downloadState: DownloadState,
        downloadedBytes: Long = 0,
        downloadedLocation: String? = null
    ) = viewModelScope.launch {
        messageListManager.updateMessageMediaDownloadState(
            messageIndex,
            downloadState,
            downloadedBytes,
            downloadedLocation
        )
    }

    fun openMessageMedia(messageIndex: Long) = viewModelScope.launch {
        Timber.d("Start file download for message index $messageIndex")
        updateMessageMediaDownloadStatus(messageIndex, DownloadState.DOWNLOADING)

        val sourceUriResult = runCatching { Uri.parse(messageListManager.getMediaContentTemporaryUrl(messageIndex)) }
        val sourceUri = sourceUriResult.getOrElse { e ->
            Timber.w(e, "Message media download failed: cannot get sourceUri")
            updateMessageMediaDownloadStatus(messageIndex, DownloadState.ERROR)
            return@launch
        }

        openMedia.value = sourceUri
    }

    private fun observeMessageMediaDownload(messageIndex: Long, downloadId: Long) {
        val downloadManager = applicationContextHolder.applicationContext.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
        val downloadCursor = downloadManager.queryById(downloadId)
        val downloadObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (!updateMessageMediaDownloadState(messageIndex, downloadId)) {
                    Timber.d("Download $downloadId completed")
                    downloadCursor.unregisterContentObserver(this)
                    downloadCursor.close()
                }
            }
        }
        downloadCursor.registerContentObserver(downloadObserver)
    }

    /**
     * Notifies the view model of the current download state
     * @return true if the download is still in progress
     */
    private fun updateMessageMediaDownloadState(messageIndex: Long, downloadId: Long): Boolean {
        val downloadManager = applicationContextHolder.applicationContext.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = downloadManager.queryById(downloadId)

        if (!cursor.moveToFirst()) {
            cursor.close()
            return false
        }

        val status = cursor.getInt(DownloadManager.COLUMN_STATUS)
        val downloadInProgress = status != DownloadManager.STATUS_FAILED && status != DownloadManager.STATUS_SUCCESSFUL
        val downloadedBytes = cursor.getLong(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
        Timber.d("Download status changed. Status: $status, downloaded bytes: $downloadedBytes")

        updateMessageMediaDownloadStatus(messageIndex, DownloadState.DOWNLOADING, downloadedBytes)

        when (status) {
            DownloadManager.STATUS_SUCCESSFUL -> {
                val downloadedFile = cursor.getString(DownloadManager.COLUMN_LOCAL_URI).toUri().toFile()
                val downloadedLocation =
                    FileProvider.getUriForFile(applicationContextHolder.applicationContext, "com.toters.twilio_chat_module.fileprovider", downloadedFile)
                        .toString()
                updateMessageMediaDownloadStatus(
                    messageIndex,
                    DownloadState.COMPLETED,
                    downloadedBytes,
                    downloadedLocation
                )
            }
            DownloadManager.STATUS_FAILED -> {
                onMessageError.value = ConversationsError.MESSAGE_MEDIA_DOWNLOAD_FAILED
                updateMessageMediaDownloadStatus(messageIndex, DownloadState.ERROR, downloadedBytes)
                Timber.w(
                    "Message media download failed. Failure reason: %s",
                    cursor.getString(DownloadManager.COLUMN_REASON)
                )
            }
        }

        cursor.close()
        return downloadInProgress
    }

    private val ParticipantDataItem.typingIndicatorName get() = friendlyName.ifEmpty { identity }
}
