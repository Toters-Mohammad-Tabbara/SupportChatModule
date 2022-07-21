package com.toters.twilio_chat_module.di

import android.content.Context
import com.toters.twilio_chat_module.ConversationsClientWrapper
import com.toters.twilio_chat_module.FCMTokenStorage
import com.toters.twilio_chat_module.manager.*
import com.toters.twilio_chat_module.repository.ConversationsRepositoryImpl
import com.toters.twilio_chat_module.viewModels.ConversationDetailsViewModel
import com.toters.twilio_chat_module.viewModels.LoginViewModel
import com.toters.twilio_chat_module.viewModels.MessageListViewModel
import com.toters.twilio_chat_module.viewModels.ParticipantListViewModel
import com.twilio.messaging.internal.ApplicationContextHolder

var injector = Injector()
    private set

open class Injector {

    private var fcmManagerImpl: FCMManagerImpl? = null

    open fun createFCMTokenStorage(applicationContext: Context) = FCMTokenStorage(applicationContext)


    open fun createChatViewModel(conversationSid: String?): ChatViewModel = ChatViewModel(
        conversationSid
    )

    open fun createLoginManager(applicationContext: Context): LoginManager = LoginManagerImpl(
        ConversationsClientWrapper.INSTANCE,
        ConversationsRepositoryImpl.INSTANCE,
        FCMTokenStorage(applicationContext),
        FirebaseTokenManager(),
    )

    open fun createLoginViewModel(applicationContext: Context): LoginViewModel {
        val loginManager = createLoginManager(applicationContext)
        val connectivityMonitor = ConnectivityMonitorImpl(applicationContext)

        return LoginViewModel(loginManager, connectivityMonitor)
    }

    open fun createMessageListViewModel(appContext: Context, conversationSid: String): MessageListViewModel {
        val messageListManager = MessageListManagerImpl(
            conversationSid,
            ConversationsClientWrapper.INSTANCE,
            ConversationsRepositoryImpl.INSTANCE
        )
        ApplicationContextHolder.createInstance(appContext)
        return MessageListViewModel(
            ApplicationContextHolder.getInstance(),
            conversationSid,
            ConversationsRepositoryImpl.INSTANCE,
            messageListManager
        )
    }

    open fun createConversationDetailsViewModel(conversationSid: String): ConversationDetailsViewModel {
        val conversationListManager = ConversationListManagerImpl(ConversationsClientWrapper.INSTANCE)
        val participantListManager = ParticipantListManagerImpl(conversationSid, ConversationsClientWrapper.INSTANCE)
        return ConversationDetailsViewModel(
            conversationSid,
            ConversationsRepositoryImpl.INSTANCE,
            conversationListManager,
            participantListManager
        )
    }

    open fun createParticipantListViewModel(conversationSid: String): ParticipantListViewModel {
        return ParticipantListViewModel(conversationSid, ConversationsRepositoryImpl.INSTANCE)
    }

    open fun createFCMManager(context: Context): FCMManager {
        val credentialStorage = createFCMTokenStorage(context.applicationContext)
        if (fcmManagerImpl == null) {
            fcmManagerImpl = FCMManagerImpl(context, ConversationsClientWrapper.INSTANCE, credentialStorage)
        }
        return fcmManagerImpl!!
    }
}
