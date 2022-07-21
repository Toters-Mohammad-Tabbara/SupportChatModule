package com.toters.twilio_chat_module.models

data class ReactionAttributes(val reactions: Map<String, Set<String>> = mapOf())
