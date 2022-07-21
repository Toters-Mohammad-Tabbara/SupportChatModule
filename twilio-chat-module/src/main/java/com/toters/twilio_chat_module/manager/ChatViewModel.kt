package com.toters.twilio_chat_module.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toters.twilio_chat_module.ConversationsClientWrapper
import com.toters.twilio_chat_module._initilizer.ConversationConfigs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val defaultConversationSid: String?) : ViewModel() {

    private val _conversationSid by lazy { MutableStateFlow(defaultConversationSid ?: "") }
    val conversationSid: StateFlow<String> = _conversationSid

    fun fetchConversationSid(supportExperienceId: Int) {
        viewModelScope.launch {
            if (defaultConversationSid.isNullOrEmpty()) {
                val fetchChatData =
                    ConversationsClientWrapper.INSTANCE.chatCallback?.fetchChatData(supportExperienceId)

                ConversationConfigs.bindConversationData(
                    fetchChatData?.sid ?: "",
                    fetchChatData?.supportExperienceId ?: 0,
                    fetchChatData?.supportTicketId ?: 0
                )
                _conversationSid.value = fetchChatData?.sid ?: defaultConversationSid ?: ""
            }
        }
    }

    fun reInitiateChat() {
        viewModelScope.launch {
            if(ConversationConfigs.featureFlags.canInitiateChat) {
                ConversationsClientWrapper.INSTANCE.chatCallback?.reInitiateChat()
            }
        }
    }

    fun rateExperience(messageSid: String, experienceId: Int, rating: Int) {
        viewModelScope.launch {
            ConversationsClientWrapper.INSTANCE.chatCallback?.rateExperience(
                messageSid,
                experienceId,
                rating
            )
        }
    }

}
