package com.toters.twilio_chat_module.extensions

import com.toters.twilio_chat_module.enums.Direction
import com.twilio.conversations.Message
import java.lang.Exception


const val UUID_KEY = "uuid"
const val IS_CHAT_END_KEY = "is_chat_end"
const val IS_CHAT_START_KEY = "is_chat_start"
const val AGENT_JUST_JOINED = "agent_just_joined"
const val IS_OUTBOUND_CHAT_END = "is_outbound_chat_end"

fun Message?.isEndChatMessage(): Boolean {
    return try {
        this?.attributes?.jsonObject?.let {
            it.has(IS_CHAT_END_KEY) && it.getBoolean(IS_CHAT_END_KEY)
        } == true
    }catch (e: Exception){
        e.printStackTrace()
        false
    }
}

fun Message?.isChatStart(): Boolean {
    return try {
        this?.attributes?.jsonObject?.let {
            it.has(IS_CHAT_START_KEY) && it.getBoolean(IS_CHAT_START_KEY)
        } == true
    }catch (e: Exception){
        e.printStackTrace()
        false
    }
}

fun Message?.getUUID(): String {
    return try {
        this?.attributes?.jsonObject?.let {
            if (it.has(UUID_KEY)) {
                it.getString(UUID_KEY)
            } else {
                ""
            }
        } ?: ""
    } catch (e: Exception){
        e.printStackTrace()
        ""
    }
}

fun Message?.didAgentJoinChat(): Boolean {
    return try {
        this?.attributes?.jsonObject?.let {
            it.has(AGENT_JUST_JOINED) && it.getBoolean(AGENT_JUST_JOINED)
        } == true
    }catch (e: Exception){
        e.printStackTrace()
        false
    }
}

fun Message?.isOutboundChatEnded():Boolean {
    return try {
        this?.attributes?.jsonObject?.let {
            it.has(IS_OUTBOUND_CHAT_END) && it.getBoolean(IS_OUTBOUND_CHAT_END)
        } == true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}