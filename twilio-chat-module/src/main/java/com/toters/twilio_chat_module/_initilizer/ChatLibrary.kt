package com.toters.twilio_chat_module._initilizer

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.toters.twilio_chat_module.ConversationsClientWrapper
import com.toters.twilio_chat_module.viewModels.LoginViewModel
import com.toters.twilio_chat_module.worker.UnSentWorker

object ChatLibrary {
    @JvmStatic
    fun addConversationUpdateListener(block: () -> Unit) {
        ConversationsClientWrapper.INSTANCE.addConversationUpdateListener {
            block()
        }
    }

    @JvmStatic
    fun retrySignIn(loginViewModel: LoginViewModel) {
        if (ConversationsClientWrapper.INSTANCE.isClientCreated.not()) {
            loginViewModel.signIn {
                ConversationsClientWrapper.INSTANCE.chatCallback?.fetchToken() ?: ""
            }
        }
    }

    @JvmStatic
    fun markUnsentMessages(context: Context) {
        val unSentWorkerRequest: WorkRequest =
            OneTimeWorkRequestBuilder<UnSentWorker>()
                .build()
        WorkManager
            .getInstance(context)
            .enqueue(unSentWorkerRequest)
    }

}