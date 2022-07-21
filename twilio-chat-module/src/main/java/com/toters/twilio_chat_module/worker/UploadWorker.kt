package com.toters.twilio_chat_module.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.toters.twilio_chat_module.enums.SendStatus
import com.toters.twilio_chat_module.repository.ConversationsRepositoryImpl

class UnSentWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    private val conversationsRepository = ConversationsRepositoryImpl.INSTANCE

    override fun doWork(): Result {

        val messageUuids = conversationsRepository.getSendingMessages().map { it.uuid }
        messageUuids.forEach { messageUuid ->
            conversationsRepository.updateMessageStatus(
                messageUuid,
                SendStatus.UNSENT.value,
                0
            )
        }

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}