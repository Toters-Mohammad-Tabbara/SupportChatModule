package com.toters.twilio_chat_module.extensions

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

var CHAT_HEADER_FORMAT = "EEEE, LLLL d"

fun getDateDiff(format: String, oldDate: String, newDate: String): Long {
    val dateFormat = SimpleDateFormat(format, Locale.getDefault())
    return try {
        TimeUnit.DAYS.convert(
            dateFormat.parse(newDate)!!.time - dateFormat.parse(oldDate)!!.time,
            TimeUnit.MILLISECONDS
        )
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
}

fun getTimeFromDateForChat(date: Date, pattern: String): String {
    val format = SimpleDateFormat(pattern, Locale.getDefault())
    var convertedDate = ""
    try {
        convertedDate = format.format(date)
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }
    return convertedDate
}