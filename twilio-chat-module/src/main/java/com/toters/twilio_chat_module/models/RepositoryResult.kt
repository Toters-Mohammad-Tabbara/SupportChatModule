package com.toters.twilio_chat_module.models

data class RepositoryResult<T>(
    val data: T,
    val requestStatus: RepositoryRequestStatus
)
