package com.toters.twilio_chat_module.extensions

import com.google.gson.annotations.SerializedName

data class ExperienceData(
    @SerializedName("support_experience_id")
    val experienceId: Int = 0,
    @SerializedName("support_experience_rating")
    val experienceRating: Int = 0
)