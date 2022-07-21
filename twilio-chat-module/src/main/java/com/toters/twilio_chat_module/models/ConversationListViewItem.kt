package com.toters.twilio_chat_module.models

data class ConversationListViewItem constructor(
    val sid: String,
    val name: String,
    val participantCount: Int,
    val unreadMessageCount: String,
    val showUnreadMessageCount: Boolean,
    val participatingStatus: Int,
    val lastMessageStateIcon: Int,
    val lastMessageText: String,
    val lastMessageColor: Int,
    val lastMessageDate: String,
    val isMuted: Boolean = false,
    val isLoading: Boolean = false
)
