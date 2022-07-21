package com.toters.twilio_chat_module.manager

import com.toters.twilio_chat_module.ConversationsClientWrapper
import com.toters.twilio_chat_module.extensions.setFriendlyName

interface UserManager {
    suspend fun setFriendlyName(friendlyName:String)
}

class UserManagerImpl(private val conversationsClient: ConversationsClientWrapper) : UserManager {

    override suspend fun setFriendlyName(friendlyName: String)
            = conversationsClient.getConversationsClient().myUser.setFriendlyName(friendlyName)

}
