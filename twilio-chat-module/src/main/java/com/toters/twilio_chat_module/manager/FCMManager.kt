package com.toters.twilio_chat_module.manager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.toters.twilio_chat_module.ConversationsClientWrapper
import com.toters.twilio_chat_module.FCMTokenStorage
import com.toters.twilio_chat_module.R
import com.toters.twilio_chat_module._initilizer.ConversationConfigs
import com.toters.twilio_chat_module.extensions.registerFCMToken
import com.toters.twilio_chat_module.ui.ChatActivity
import com.twilio.conversations.ConversationsException
import com.twilio.conversations.NotificationPayload
import timber.log.Timber

private const val NOTIFICATION_CONVERSATION_ID = "chat_notification_id"
private const val NOTIFICATION_NAME = "Chat Notification"
private const val NOTIFICATION_ID = 1234

interface FCMManager : DefaultLifecycleObserver {
    suspend fun onNewToken(token: String)
    suspend fun onMessageReceived(appName: String, data: MutableMap<String, String>)
    fun showNotification(appName: String, payload: NotificationPayload)
}

class FCMManagerImpl(
    private val context: Context,
    private val conversationsClient: ConversationsClientWrapper,
    private val fcmTokenStorage: FCMTokenStorage,
) : FCMManager, DefaultLifecycleObserver {

    private var inboxStyle = NotificationCompat.InboxStyle()

    private val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override suspend fun onNewToken(token: String) {
        Timber.d("FCM Token received: $token")
        try {
            if (token != fcmTokenStorage.fcmToken && conversationsClient.isClientCreated) {
                conversationsClient.getConversationsClient().registerFCMToken(token)
            }
            fcmTokenStorage.fcmToken = token
        } catch (e: ConversationsException) {
            Timber.d("Failed to register FCM token")
        }
    }

    override suspend fun onMessageReceived(appName: String, data: MutableMap<String, String>) {
        val payload = NotificationPayload(data)
        if (conversationsClient.isClientCreated) {
            conversationsClient.getConversationsClient().handleNotification(payload)
        }
        Timber.d("Message received: $payload, ${payload.type}, ${ConversationConfigs.isChatActivityOpen}")
        // Ignore everything we don't support
        if (payload.type == NotificationPayload.Type.UNKNOWN) return

        if (!ConversationConfigs.isChatActivityOpen) {
            showNotification(appName, payload)
        }
    }

    private val NotificationPayload.textForNotification: String get() = when (type) {
        NotificationPayload.Type.NEW_MESSAGE -> when {
            mediaCount > 1 -> context.getString(R.string.notification_media_message, mediaCount)
            mediaCount > 0 -> context.getString(R.string.notification_media) + ": " +
                        mediaFilename.ifEmpty { Formatter.formatShortFileSize(context, mediaSize) }
            else -> body
        }
        else -> body
    }

    private fun buildNotification(appName: String, payload: NotificationPayload): Notification {
        val intent = getTargetIntent(payload.type, payload.conversationSid)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ONE_SHOT)

        inboxStyle.addLine(payload.textForNotification)

        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CONVERSATION_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(appName)
            .setContentText(payload.textForNotification)
            .setAutoCancel(true)
            .setPriority(PRIORITY_HIGH)
            .setVisibility(VISIBILITY_PUBLIC)
            .setStyle(inboxStyle)
            .setContentIntent(pendingIntent)

        val soundFileName = payload.sound
        if (context.resources.getIdentifier(soundFileName, "raw", context.packageName) != 0) {
            val sound = Uri.parse("android.resource://${context.packageName}/raw/$soundFileName")
            notificationBuilder.setSound(sound)
            Timber.d("Playing specified sound $soundFileName")
        } else {
            notificationBuilder.setDefaults(Notification.DEFAULT_SOUND)
            Timber.d("Playing default sound")
        }

        return notificationBuilder.build()
    }

    override fun showNotification(appName: String, payload: NotificationPayload) {
        Timber.d("showNotification: ${payload.conversationSid}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CONVERSATION_ID,
                NOTIFICATION_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notification = buildNotification(appName, payload)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun getTargetIntent(type: NotificationPayload.Type, conversationSid: String): Intent {
        return when (type) {
            NotificationPayload.Type.NEW_MESSAGE -> ChatActivity.getStartIntent(context, conversationSid)
            else -> {
                ChatActivity.getStartIntent(context, conversationSid)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
    }

    override fun onStart(owner: LifecycleOwner) {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
