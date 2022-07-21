package com.toters.twilio_chat_module._initilizer

import android.app.Application
import com.toters.twilio_chat_module.ConversationsClientWrapper

import com.toters.twilio_chat_module.localCache.LocalCacheProvider
import com.toters.twilio_chat_module.manager.ChatCallback
import com.toters.twilio_chat_module.repository.ConversationsRepositoryImpl.Companion.createInstance
import com.toters.twilio_chat_module.viewModels.LoginViewModel
import kotlinx.coroutines.Job

object ChatLibraryInitializer {

    fun init(
        application: Application,
        loginViewModel: Lazy<LoginViewModel>,
        chatCallbackImpl: ChatCallback,
        signOut: () -> Job
    ) {
        ConversationsClientWrapper.createInstance(application)
        LocalCacheProvider.createInstance(application)
        createInstance(ConversationsClientWrapper.INSTANCE, LocalCacheProvider.INSTANCE)

        ConversationsClientWrapper.INSTANCE.onUpdateTokenFailure += { signOut() }
        ConversationsClientWrapper.INSTANCE.chatCallback = chatCallbackImpl
        loginViewModel.value.signIn {
            ConversationsClientWrapper.INSTANCE.chatCallback?.fetchToken() ?: ""
        }
    }
}