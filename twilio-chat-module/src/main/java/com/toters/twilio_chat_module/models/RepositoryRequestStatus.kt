package com.toters.twilio_chat_module.models

import com.toters.twilio_chat_module.enums.ConversationsError

sealed class RepositoryRequestStatus {
    object NONE : RepositoryRequestStatus()
    object FETCHING : RepositoryRequestStatus()
    object SUBSCRIBING : RepositoryRequestStatus()
    object COMPLETE : RepositoryRequestStatus()
    class Error(val error: ConversationsError) : RepositoryRequestStatus()
}
