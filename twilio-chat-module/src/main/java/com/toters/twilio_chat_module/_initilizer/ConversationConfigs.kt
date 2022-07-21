package com.toters.twilio_chat_module._initilizer

import com.toters.twilio_chat_module.models.ConversationData

object ConversationConfigs {

    fun bindConversationData(
        conversationSid: String,
        supportExperienceIdJ: Int,
        supportTicketId: Int
    ) {
        this.conversationSid = conversationSid
        this.supportExperienceIdJ = supportExperienceIdJ
        this.supportTicketId = supportTicketId
    }

    fun getConversationData(): ConversationData {
        return ConversationData(
            conversationSid,
            supportExperienceIdJ,
            supportTicketId
        )
    }

    // ConversationData Configurations
    var conversationSid: String = ""
        private set

    var supportExperienceIdJ: Int = 0
        private set

    var supportTicketId: Int = 0
        private set

    /**
     * Internal Configs
     */

    //FeatureFlags
    internal var featureFlags = FeatureFlags()

    fun setFeatureFlags(featureFlags: FeatureFlags) {
        this.featureFlags = featureFlags
    }

    // Track screens
    internal var isChatActivityOpen: Boolean = false
}