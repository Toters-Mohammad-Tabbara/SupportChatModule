package com.toters.twilio_chat_module

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import com.toters.twilio_chat_module.extensions.addListener
import com.toters.twilio_chat_module.extensions.createAndSyncClient
import com.toters.twilio_chat_module.extensions.createClientListener
import com.toters.twilio_chat_module.extensions.updateToken
import com.toters.twilio_chat_module.manager.ChatCallback
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.ConversationsClientListener
import kotlinx.coroutines.*
import timber.log.Timber

class ConversationsClientWrapper(private val applicationContext: Context) {

    private var deferredClient = CompletableDeferred<ConversationsClient>()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    val isClientCreated get() = deferredClient.isCompleted && !deferredClient.isCancelled

    val onUpdateTokenFailure = mutableListOf<() -> Unit>()

    var chatCallback: ChatCallback? = null
    private var conversationUpdateListener: ConversationsClientListener? = null

    private fun notifyUpdateTokenFailure() = onUpdateTokenFailure.forEach { it() }

    suspend fun getConversationsClient() = deferredClient.await() // Business logic will wait until conversationsClient created

    /**
     * Get token and call createClient if token is not null
     */
    suspend fun create(fetchToken: suspend () -> String) {
        Timber.d("create")

        val token = fetchToken()
        Timber.d("token: $token")

        val client = createAndSyncClient(applicationContext, token)
        this.deferredClient.complete(client)

        client.addListener(
            onTokenAboutToExpire = { updateToken(fetchToken, notifyOnFailure = false) },
            onTokenExpired = { updateToken(fetchToken, notifyOnFailure = true) },
        )
    }

    suspend fun shutdown() {
        Timber.d("shutdown")
        getConversationsClient().shutdown()
        deferredClient = CompletableDeferred()
    }

    private fun updateToken(fetchToken: suspend () -> String, notifyOnFailure: Boolean) = coroutineScope.launch {
        Timber.d("updateToken notifyOnFailure: $notifyOnFailure")

        val result = runCatching {
            val twilioToken = fetchToken()
            getConversationsClient().updateToken(twilioToken)
        }

        if (result.isFailure && notifyOnFailure) {
            Timber.e(result.exceptionOrNull())
            notifyUpdateTokenFailure()
        }
    }

    fun addConversationUpdateListener(onConversationUpdated: () -> Unit) {
        coroutineScope.launch {
            if (conversationUpdateListener == null) {
                conversationUpdateListener = createClientListener(
                    onConversationUpdated = { _, _ ->
                        coroutineScope.launch {
                            onConversationUpdated()
                        }
                    }
                )
                getConversationsClient().addListener(conversationUpdateListener!!)
            }
        }
    }

    companion object {

        val INSTANCE get() = _instance ?: error("call ConversationsClientWrapper.createInstance() first")

        private var _instance: ConversationsClientWrapper? = null

        fun createInstance(applicationContext: Context) {
            check(_instance == null) { "ConversationsClientWrapper singleton instance has been already created" }
            _instance = ConversationsClientWrapper(applicationContext)
        }

        @DelicateCoroutinesApi
        @RestrictTo(Scope.TESTS)
        fun recreateInstance(applicationContext: Context) {
            _instance?.let { instance ->
                // Shutdown old client if it will ever be created
                GlobalScope.launch { instance.getConversationsClient().shutdown() }
            }

            _instance = null
            createInstance(applicationContext)
        }
    }
}
