package com.toters.twilio_chat_module.enums

enum class SendStatus(val value: Int) {
    UNDEFINED(0),
    SENDING(1),
    SENT(2),
    UNSENT(3),
    ERROR(4);

    companion object {
        private val valuesMap = values().associateBy { it.value }
        fun fromInt(value: Int) = valuesMap[value] ?: error("Invalid value $value for SendStatus")
    }
}
