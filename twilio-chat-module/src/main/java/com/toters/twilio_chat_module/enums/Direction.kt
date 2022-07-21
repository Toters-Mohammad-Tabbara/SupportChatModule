package com.toters.twilio_chat_module.enums

enum class Direction(val value: Int) {
    INCOMING(0),
    OUTGOING(1),
    SYSTEM(2),
    INBOUND_JOINED(3),
    INBOUND_END(4),
    OUTBOUND_END(5);

    companion object {
        private val valuesMap = values().associateBy { it.value }
        fun fromInt(value: Int) = valuesMap[value] ?: error("Invalid value $value for Direction")
    }
}
