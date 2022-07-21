package com.toters.twilio_chat_module.models

import android.net.Uri
import com.toters.twilio_chat_module.enums.Direction
import com.toters.twilio_chat_module.enums.DownloadState
import com.toters.twilio_chat_module.enums.MessageType
import com.toters.twilio_chat_module.enums.SendStatus
import com.toters.twilio_chat_module.extensions.ExperienceData

data class MessageListViewItem constructor(
    val sid: String,
    val uuid: String,
    val index: Long,
    val previousDirection: Direction,
    val direction: Direction,
    val author: String,
    val authorChanged: Boolean,
    val body: String,
    val epochDateCreated: Long,
    val dateChanged: Boolean,
    val sendStatus: SendStatus,
    val sendStatusIcon: Int,
    val experienceData: ExperienceData,
    val type: MessageType,
    val mediaSid: String?,
    val mediaFileName: String?,
    val mediaType: String?,
    val mediaSize: Long?,
    val mediaUri: Uri?,
    val mediaDownloadId: Long?,
    val mediaDownloadedBytes: Long?,
    val mediaDownloadState: DownloadState,
    val mediaUploading: Boolean,
    val mediaUploadedBytes: Long?,
    val mediaUploadUri: Uri?,
    val errorCode: Int
)
