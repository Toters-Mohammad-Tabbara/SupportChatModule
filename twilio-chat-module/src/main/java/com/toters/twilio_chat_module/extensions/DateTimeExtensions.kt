package com.toters.twilio_chat_module.extensions

import android.content.Context
import androidx.core.content.ContextCompat
import com.toters.twilio_chat_module.R
import com.toters.twilio_chat_module.enums.SendStatus
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import java.text.SimpleDateFormat
import java.util.*

fun Long.asTimeString(): String = SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(this))

fun Long.asDateString(): String = SimpleDateFormat(CHAT_HEADER_FORMAT, Locale.getDefault()).format(Date(this))

fun Long.asMessageCount(): String = if (this > 99) "99+" else this.toString()

fun Long.asLastMessageDateString(context: Context) : String {
    if (this == 0L) {
        return ""
    }

    val instant = Instant.fromEpochMilliseconds(this)
    val now = Clock.System.now()
    val timeZone = TimeZone.currentSystemDefault()

    return when (instant.daysUntil(now, timeZone)) {
        1 -> context.getString(R.string.yesterday)
        0 -> context.getString(R.string.today)
        else -> asTimeString() // today
    }
}

fun SendStatus.asLastMessageStatusIcon() = when(this) {
    SendStatus.SENDING -> R.drawable.ic_message_timer
    SendStatus.SENT -> R.drawable.ic_message_sent
    SendStatus.ERROR -> R.drawable.ic_failed_message
    else -> 0
}

fun SendStatus.asLastMessageTextColor(context: Context) = when (this) {
    SendStatus.ERROR -> ContextCompat.getColor(context, R.color.redColor)
    else -> ContextCompat.getColor(context, R.color.text_subtitle)
}
