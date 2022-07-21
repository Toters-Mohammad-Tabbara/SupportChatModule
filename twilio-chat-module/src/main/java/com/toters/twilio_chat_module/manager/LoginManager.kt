package com.toters.twilio_chat_module.manager

import com.toters.twilio_chat_module.ConversationsClientWrapper
import com.toters.twilio_chat_module.FCMTokenStorage
import com.toters.twilio_chat_module.repository.ConversationsRepository
import com.toters.twilio_chat_module.extensions.registerFCMToken
import timber.log.Timber

interface LoginManager {
    suspend fun signIn(fetchToken: suspend () -> String)
    suspend fun signOut()
    suspend fun registerForFcm()
    suspend fun unregisterFromFcm()
    fun clearCredentials()
}

class LoginManagerImpl(
    private val conversationsClient: ConversationsClientWrapper,
    private val conversationsRepository: ConversationsRepository,
    private val fcmTokenStorage: FCMTokenStorage,
    private val firebaseTokenManager: FirebaseTokenManager,
) : LoginManager {

    override suspend fun registerForFcm() {
        try {
            val token = firebaseTokenManager.retrieveToken()
            fcmTokenStorage.fcmToken = token
            Timber.d("Registering for FCM: $token")
            conversationsClient.getConversationsClient().registerFCMToken(token)
        } catch (e: Exception) {
            Timber.d(e, "Failed to register FCM")
        }
    }

    override suspend fun unregisterFromFcm() {
        // We don't call `conversationsClient.getConversationsClient().unregisterFCMToken(token)` here
        // because it fails with commandTimeout (60s by default) if device is offline or token is expired.
        // Instead we try to delete token on FCM async. Which leads to the same result if device is online,
        // but we can shutdown `conversationsClient`immediately without waiting a result.
        firebaseTokenManager.deleteToken()
    }

    override suspend fun signIn(fetchToken: suspend () -> String) {
        Timber.d("signIn")
        conversationsClient.create(fetchToken)
        conversationsRepository.subscribeToConversationsClientEvents()
        registerForFcm()
    }

    override suspend fun signOut() {
        unregisterFromFcm()
        clearCredentials()
        conversationsRepository.unsubscribeFromConversationsClientEvents()
        conversationsRepository.clear()
        conversationsClient.shutdown()
    }

    override fun clearCredentials() {
        fcmTokenStorage.clearCredentials()
    }
}
