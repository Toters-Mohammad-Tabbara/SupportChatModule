package com.toters.twilio_chat_module.manager

import android.content.Context
import com.toters.twilio_chat_module.models.ConversationData

interface ChatCallback {
    suspend fun fetchToken(): String

    suspend fun fetchIsChatActive(): Boolean

    suspend fun fetchChatData(supportExperienceId: Int):ConversationData?

    suspend fun rateExperience(messageSid: String, experienceId: Int, rating: Int)

    suspend fun initiateChat(orderId: Int, supportReasonId: Int): Boolean

    suspend fun reInitiateChat()

    fun onBackPressed(context: Context)
}
