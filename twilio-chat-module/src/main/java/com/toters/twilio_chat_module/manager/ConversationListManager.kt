package com.toters.twilio_chat_module.manager

import com.toters.twilio_chat_module.ConversationsClientWrapper
import com.toters.twilio_chat_module.extensions.*


interface ConversationListManager {
    suspend fun createConversation(friendlyName: String): String
    suspend fun joinConversation(conversationSid: String)
    suspend fun removeConversation(conversationSid: String)
    suspend fun leaveConversation(conversationSid: String)
    suspend fun muteConversation(conversationSid: String)
    suspend fun unmuteConversation(conversationSid: String)
    suspend fun renameConversation(conversationSid: String, friendlyName: String)
}

class ConversationListManagerImpl(private val conversationsClient: ConversationsClientWrapper) :
    ConversationListManager {

    override suspend fun createConversation(friendlyName: String): String
            = conversationsClient.getConversationsClient().createConversation(friendlyName).sid

    override suspend fun joinConversation(conversationSid: String): Unit
            = conversationsClient.getConversationsClient().getConversation(conversationSid).join()

    override suspend fun removeConversation(conversationSid: String): Unit
            = conversationsClient.getConversationsClient().getConversation(conversationSid).destroy()

    override suspend fun leaveConversation(conversationSid: String): Unit
            = conversationsClient.getConversationsClient().getConversation(conversationSid).leave()

    override suspend fun muteConversation(conversationSid: String): Unit
            = conversationsClient.getConversationsClient().getConversation(conversationSid).muteConversation()

    override suspend fun unmuteConversation(conversationSid: String): Unit
            = conversationsClient.getConversationsClient().getConversation(conversationSid).unmuteConversation()

    override suspend fun renameConversation(conversationSid: String, friendlyName: String)
            = conversationsClient.getConversationsClient().getConversation(conversationSid).setFriendlyName(friendlyName)

}
