package com.toters.twilio_chat_module.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toters.twilio_chat_module._common.SingleLiveEvent
import com.toters.twilio_chat_module.enums.ConversationsError
import com.toters.twilio_chat_module.extensions.asParticipantListViewItems
import com.toters.twilio_chat_module.models.ParticipantListViewItem
import com.toters.twilio_chat_module.models.RepositoryRequestStatus
import com.toters.twilio_chat_module.repository.ConversationsRepository
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.properties.Delegates

class ParticipantListViewModel constructor(
    val conversationSid: String,
    private val conversationsRepository: ConversationsRepository
) : ViewModel() {

    private var unfilteredParticipantsList by Delegates.observable(listOf<ParticipantListViewItem>()) { _, _, _ ->
        updateParticipantList()
    }
    val participantsList = MutableLiveData<List<ParticipantListViewItem>>(emptyList())

    private val _agentInChat = MutableLiveData(false)
    val agentInChat: LiveData<Boolean> = _agentInChat

    var participantFilter by Delegates.observable("") { _, _, _ ->
        updateParticipantList()
    }
    val onParticipantError = SingleLiveEvent<ConversationsError>()

    private fun updateParticipantList() {
        participantsList.value = unfilteredParticipantsList.filterByName(participantFilter)
        viewModelScope.launch {
            val myIdentity = conversationsRepository.getMyIdentity().single()
            _agentInChat.value = participantsList.value?.any { it.identity != myIdentity }
        }
    }

    private fun List<ParticipantListViewItem>.filterByName(name: String): List<ParticipantListViewItem> =
        if (name.isEmpty()) {
            this
        } else {
            filter {
                it.friendlyName.contains(name, ignoreCase = true)
            }
        }

    private fun getConversationParticipants() = viewModelScope.launch {
        conversationsRepository.getConversationParticipants(conversationSid).collect { (list, status) ->
            unfilteredParticipantsList = list.asParticipantListViewItems()
            if (status is RepositoryRequestStatus.Error) {
                onParticipantError.value = ConversationsError.PARTICIPANTS_FETCH_FAILED
            }
        }
    }
}
