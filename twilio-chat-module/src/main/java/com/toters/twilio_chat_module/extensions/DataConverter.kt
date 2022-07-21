package com.toters.twilio_chat_module.extensions

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.net.toUri
import com.google.gson.Gson
import com.toters.twilio_chat_module.R
import com.toters.twilio_chat_module.enums.*
import com.toters.twilio_chat_module.localCache.entity.ConversationDataItem
import com.toters.twilio_chat_module.localCache.entity.MessageDataItem
import com.toters.twilio_chat_module.localCache.entity.ParticipantDataItem
import com.toters.twilio_chat_module.manager.friendlyName
import com.toters.twilio_chat_module.models.*
import com.twilio.conversations.Conversation
import com.twilio.conversations.Conversation.NotificationLevel
import com.twilio.conversations.Message
import com.twilio.conversations.Participant
import com.twilio.conversations.User
import java.util.*

fun Conversation.toConversationDataItem(): ConversationDataItem {
    return ConversationDataItem(
        this.sid,
        this.friendlyName,
        this.attributes.toString(),
        this.uniqueName,
        this.dateUpdatedAsDate?.time ?: 0,
        this.dateCreatedAsDate?.time ?: 0,
        0,
        "",
        SendStatus.UNDEFINED.value,
        this.createdBy,
        0,
        0,
        0,
        this.status.value,
        this.notificationLevel.value
    )
}

fun Message.toMessageDataItem(currentUserIdentity: String = participant.identity, uuid: String = ""): MessageDataItem {
    val media = firstMedia  // @todo: support multiple media
    return MessageDataItem(
        this.sid,
        this.conversationSid,
        this.participantSid,
        if (media != null) MessageType.MEDIA.value else MessageType.TEXT.value,
        this.author,
        this.dateCreatedAsDate.time,
        this.body ?: "",
        this.messageIndex,
        this.attributes.toString(),
        this.computeDirection(currentUserIdentity),
        if (this.author == currentUserIdentity) SendStatus.SENT.value else SendStatus.UNDEFINED.value,
        uuid,
        media?.sid,
        media?.filename,
        media?.contentType,
        media?.size
    )
}

private fun Message.computeDirection(currentUserIdentity: String): Int {
    return when {
        this.didAgentJoinChat() -> {
            Direction.INBOUND_JOINED.value
        }
        this.isEndChatMessage() -> {
            Direction.INBOUND_END.value
        }
        this.isOutboundChatEnded() -> {
            Direction.OUTBOUND_END.value
        }
        this.isChatStart() -> {
            Direction.SYSTEM.value
        }
        this.author == currentUserIdentity -> {
            Direction.OUTGOING.value
        }
        else -> {
            Direction.INCOMING.value
        }
    }
}

fun MessageDataItem.toMessageListViewItem(previousDirection: Int, authorChanged: Boolean, dateChanged: Boolean): MessageListViewItem {
    return MessageListViewItem(
        this.sid,
        this.uuid,
        this.index,
        Direction.fromInt(previousDirection),
        Direction.fromInt(this.direction),
        this.author,
        authorChanged,
        this.body ?: "",
        this.dateCreated,
        dateChanged,
        SendStatus.fromInt(sendStatus),
        sendStatusIcon = SendStatus.fromInt(this.sendStatus).asLastMessageStatusIcon(),
        getExperienceData(attributes),
        MessageType.fromInt(this.type),
        this.mediaSid,
        this.mediaFileName,
        this.mediaType,
        this.mediaSize,
        this.mediaUri?.toUri(),
        this.mediaDownloadId,
        this.mediaDownloadedBytes,
        DownloadState.fromInt(this.mediaDownloadState),
        this.mediaUploading,
        this.mediaUploadedBytes,
        this.mediaUploadUri?.toUri(),
        this.errorCode
    )
}

fun getExperienceData(attributes: String): ExperienceData = try {
    Gson().fromJson(attributes, ExperienceData::class.java)
} catch (e: Exception) {
    ExperienceData()
}

fun getReactions(attributes: String): Map<String, Set<String>> = try {
    Gson().fromJson(attributes, ReactionAttributes::class.java).reactions
} catch (e: Exception) {
    emptyMap()
}

fun Participant.asParticipantDataItem(typing: Boolean = false, user: User? = null) = ParticipantDataItem(
    sid = this.sid,
    conversationSid = this.conversation.sid,
    identity = this.identity,
    friendlyName = user?.friendlyName?.takeIf { it.isNotEmpty() } ?: this.friendlyName ?: this.identity,
    isOnline = user?.isOnline ?: false,
    lastReadMessageIndex = this.lastReadMessageIndex,
    lastReadTimestamp = this.lastReadTimestamp,
    typing = typing
)

fun User.asUserViewItem() = UserViewItem(
    friendlyName = this.friendlyName,
    identity = this.identity
)

fun ConversationDataItem.asConversationListViewItem(
    context: Context,
) = ConversationListViewItem(
    this.sid,
    this.friendlyName.ifEmpty { this.sid },
    this.participantsCount.toInt(),
    this.unreadMessagesCount.asMessageCount(),
    showUnreadMessageCount = this.unreadMessagesCount > 0,
    this.participatingStatus,
    lastMessageStateIcon = SendStatus.fromInt(this.lastMessageSendStatus).asLastMessageStatusIcon(),
    this.lastMessageText,
    lastMessageColor = SendStatus.fromInt(this.lastMessageSendStatus).asLastMessageTextColor(context),
    this.lastMessageDate.asLastMessageDateString(context),
    isMuted = this.notificationLevel == NotificationLevel.MUTED.value
)

fun ConversationDataItem.asConversationDetailsViewItem() = ConversationDetailsViewItem(
    this.sid,
    this.friendlyName,
    this.createdBy,
    this.dateCreated.asDateString(),
    this.notificationLevel == NotificationLevel.MUTED.value
)

fun ParticipantDataItem.toParticipantListViewItem() = ParticipantListViewItem(
    conversationSid = this.conversationSid,
    sid = this.sid,
    identity = this.identity,
    friendlyName = this.friendlyName,
    isOnline = this.isOnline
)

fun List<ConversationDataItem>.asConversationListViewItems(context: Context) =
    map { it.asConversationListViewItem(context) }

fun MessageDataItem.asMessageListViewItems(previousMessageDataItem: MessageDataItem? = null) =
    toMessageListViewItem(
        getPreviousDirection(previousMessageDataItem),
        isAuthorChanged(previousMessageDataItem),
        isDateChanged(previousMessageDataItem)
    )


fun List<Message>.asMessageDataItems(identity: String) = map { it.toMessageDataItem(identity, it.getUUID()) }

private fun MessageDataItem.isAuthorChanged(previousMessageDataItem: MessageDataItem?): Boolean {
    if (previousMessageDataItem == null) return true
    return this.author != previousMessageDataItem.author
}

private fun MessageDataItem.isDateChanged(previousMessageDataItem: MessageDataItem?): Boolean {
    if (previousMessageDataItem == null) return true

    val dayDifference = getDateDiff(
        CHAT_HEADER_FORMAT,
        getTimeFromDateForChat(
            Date(previousMessageDataItem.dateCreated),
            CHAT_HEADER_FORMAT
        ),
        getTimeFromDateForChat(
            Date(dateCreated),
            CHAT_HEADER_FORMAT
        )
    )

    return dayDifference > 0
}

fun MessageListViewItem.sinceDate(context: Context): String {

    val dayDifference = getDateDiff(
        CHAT_HEADER_FORMAT,
        getTimeFromDateForChat(
            Date(epochDateCreated),
            CHAT_HEADER_FORMAT
        ),
        getTimeFromDateForChat(
            Calendar.getInstance().time,
            CHAT_HEADER_FORMAT
        )
    )

    return when {
        dayDifference.toInt() == 0 -> {
            context.getString(R.string.today)
        }
        dayDifference.toInt() == 1 -> {
            context.getString(R.string.yester_day_label)
        }
        dayDifference.toInt() > 1 -> {
            epochDateCreated.asDateString()
        }
        else -> ""
    }

}

private fun MessageDataItem.getPreviousDirection(previousMessageDataItem: MessageDataItem?): Int {
    return previousMessageDataItem?.direction ?: Direction.SYSTEM.value
}

fun List<ParticipantDataItem>.asParticipantListViewItems() = map { it.toParticipantListViewItem() }

fun List<ConversationListViewItem>.merge(oldConversationList: List<ConversationListViewItem>?): List<ConversationListViewItem> {
    val oldConversationMap = oldConversationList?.associate { it.sid to it } ?: return this
    return map { item ->
        val oldItem = oldConversationMap[item.sid] ?: return@map item
        item.copy(isLoading = oldItem.isLoading)
    }
}
