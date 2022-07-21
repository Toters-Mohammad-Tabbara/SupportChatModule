package com.toters.twilio_chat_module.ui

import android.graphics.Color
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.button.MaterialButton
import com.toters.twilio_chat_module.R
import com.toters.twilio_chat_module.databinding.*
import com.toters.twilio_chat_module.enums.Direction
import com.toters.twilio_chat_module.enums.DownloadState.*
import com.toters.twilio_chat_module.enums.MessageType
import com.toters.twilio_chat_module.enums.SendStatus
import com.toters.twilio_chat_module.extensions.*
import com.toters.twilio_chat_module.localCache.entity.MessageDataItem
import com.toters.twilio_chat_module.models.MessageListViewItem
import timber.log.Timber
import java.util.*

class ChatAdapter(
    private val onDisplaySendError: (message: MessageListViewItem) -> Unit,
    private val openMessageMedia: (message: MessageListViewItem) -> Unit,
    private val onRateExperience: (messageSid: String, ticketId: Int, rating: Int) -> Unit
) : PagingDataAdapter<MessageDataItem, ChatAdapter.ViewHolder>(MESSAGE_COMPARATOR) {

    fun getMessage(position: Int): MessageListViewItem? {
        return if (position == 0) {
            getItem(position)?.asMessageListViewItems()
        } else {
            getItem(position)?.asMessageListViewItems(getItem(position - 1))
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getMessage(position)?.direction?.value ?: Direction.INCOMING.value
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        when (viewType) {
            Direction.INCOMING.value -> {
                val binding = RowMessageItemIncomingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return IncomingViewHolder(binding)
            }
            Direction.OUTGOING.value -> {
                val binding = RowMessageItemOutgoingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return OutgoingViewHolder(binding)
            }
            Direction.SYSTEM.value -> {
                val binding = ChatSystemItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return SystemViewHolder(binding)
            }

            Direction.INBOUND_JOINED.value,
            Direction.INBOUND_END.value,
            Direction.OUTBOUND_END.value -> {
                val binding = ChatJoinLeftItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return JoinLeftViewHolder(binding)
            }
            else -> {
                val binding = RowMessageItemOutgoingBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return OutgoingViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = getMessage(position)
        if (message == null) {
            Timber.e("onBindViewHolder called for a missing item (position: $position, total items: $itemCount)")
            return
        }
        holder.initData(
            message,
            onDisplaySendError,
            openMessageMedia,
            onRateExperience
        )
        holder.bindData()
    }

    sealed class ViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {

        internal var onRateExperience: (String, Int, Int) -> Unit = { _,_,_ -> }
        internal var attachmentInfoText: String? = null
        internal var attachmentInfoColor: Int = 0
        internal var attachmentOnClickListener: View.OnClickListener? = null
        internal var onDisplaySendErrorListener: View.OnClickListener? = null
        internal var message: MessageListViewItem? = null

        fun initData(
            message: MessageListViewItem,
            onDisplaySendError: (message: MessageListViewItem) -> Unit,
            openMessageMedia: (message: MessageListViewItem) -> Unit,
            onRateExperience: (messageSid: String, ticketId: Int, rating: Int) -> Unit
        ) {
            val context = itemView.context

            this.message = message
            this.onRateExperience = onRateExperience

            val mediaSize = Formatter.formatShortFileSize(context, message.mediaSize ?: 0)
            val mediaUploadedBytes =
                Formatter.formatShortFileSize(context, message.mediaUploadedBytes ?: 0)
            val mediaDownloadedBytes =
                Formatter.formatShortFileSize(context, message.mediaDownloadedBytes ?: 0)

            attachmentInfoText = when {
                message.sendStatus == SendStatus.ERROR -> context.getString(R.string.err_failed_to_upload_media)

                message.mediaUploading -> context.getString(
                    R.string.attachment_uploading,
                    mediaUploadedBytes
                )

                message.mediaUploadUri != null ||
                        message.mediaDownloadState == COMPLETED -> context.getString(R.string.attachment_tap_to_open)

                message.mediaDownloadState == NOT_STARTED -> mediaSize

                message.mediaDownloadState == DOWNLOADING -> context.getString(
                    R.string.attachment_downloading,
                    mediaDownloadedBytes
                )

                message.mediaDownloadState == ERROR -> context.getString(R.string.err_failed_to_download_media)

                else -> error("Never happens")
            }

            attachmentInfoColor = when {
                message.sendStatus == SendStatus.ERROR ||
                        message.mediaDownloadState == ERROR -> ContextCompat.getColor(
                    context,
                    R.color.redColor
                )

                message.mediaUploading -> ContextCompat.getColor(context, R.color.text_subtitle)

                message.mediaUploadUri != null ||
                        message.mediaDownloadState == COMPLETED -> ContextCompat.getColor(
                    context,
                    R.color.purple_200
                )

                else -> ContextCompat.getColor(context, R.color.text_subtitle)
            }

            attachmentOnClickListener = View.OnClickListener {
                openMessageMedia(message)
            }

            onDisplaySendErrorListener = View.OnClickListener {
                onDisplaySendError(message)
            }
        }

        abstract fun bindData()
    }

    class OutgoingViewHolder(private val binding: RowMessageItemOutgoingBinding) : ViewHolder(binding) {
        override fun bindData() {
            if (message?.dateChanged == true) {
                binding.chatDateLayout.root.isVisible = true
                binding.chatDateLayout.chatCreatedDateTextView.text =
                    message?.sinceDate(itemView.context)
            } else {
                binding.chatDateLayout.root.isVisible = false
            }

            binding.messageDateTextView.text = message?.epochDateCreated?.asTimeString()

            binding.imageIcon.isVisible =
                message?.type == MessageType.MEDIA
            if (message?.type == MessageType.MEDIA) {
                binding.messageBody.text = message?.mediaFileName
            } else {
                binding.messageBody.text = message?.body
            }
            val authorChanged = message?.authorChanged == true
            binding.icTail.isInvisible = !authorChanged && message?.dateChanged != true
            val drawable = if (authorChanged || message?.dateChanged == true) {
                ContextCompat.getDrawable(
                    itemView.context,
                    R.drawable.chat_bubble_shape_for_tail
                )
            } else {
                ContextCompat.getDrawable(
                    itemView.context,
                    R.drawable.chat_bubble_shape
                )
            }
            binding.backgroundMessageBody.setImageDrawable(drawable)
            when {
                message?.previousDirection == Direction.SYSTEM || message?.dateChanged == true -> {
                    binding.topSpace.layoutParams.height = 24.dp
                }
                message?.previousDirection == Direction.OUTGOING -> {
                    binding.topSpace.layoutParams.height = 2.dp
                }
                message?.previousDirection == Direction.INCOMING -> {
                    binding.topSpace.layoutParams.height = 16.dp
                }
                else -> {
                    binding.topSpace.layoutParams.height = 24.dp
                }
            }
            message?.let { message ->
                binding.imageMessageSent.setImageDrawable(
                    ContextCompat.getDrawable(
                        itemView.context,
                        message.sendStatusIcon
                    )
                )
            }
            binding.messageBody.setOnClickListener(
                when (message?.sendStatus) {
                    SendStatus.SENT -> {
                        attachmentOnClickListener
                    }
                    SendStatus.ERROR -> {
                        onDisplaySendErrorListener
                    }
                    else -> {
                        null
                    }
                }
            )
            if (message?.type == MessageType.MEDIA) {
                binding.messageBody.setTextColor(attachmentInfoColor)
                binding.messageBody.isClickable = true
                binding.messageBody.isEnabled = true
            } else {
                binding.messageBody.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_subtitle))
                binding.messageBody.isClickable = false
                binding.messageBody.isEnabled = false
            }
        }

    }

    class IncomingViewHolder(private val binding: RowMessageItemIncomingBinding) :
        ViewHolder(binding) {
        override fun bindData() {
            if (message?.dateChanged == true) {
                binding.chatDateLayout.root.isVisible = true
                binding.chatDateLayout.chatCreatedDateTextView.text =
                    message?.sinceDate(itemView.context)
            } else {
                binding.chatDateLayout.root.isVisible = false
            }

            binding.messageDateTextView.text = message?.epochDateCreated?.asTimeString()

            binding.imageIcon.isVisible = message?.type == MessageType.MEDIA
            if (message?.type == MessageType.MEDIA) {
                binding.messageBody.text = attachmentInfoText
            } else {
                binding.messageBody.text = message?.body
            }
            val authorChanged = message?.authorChanged == true
            binding.icTail.isInvisible = !authorChanged && message?.dateChanged != true
            val drawable = if (authorChanged || message?.dateChanged == true) {
                ContextCompat.getDrawable(
                    itemView.context,
                    R.drawable.chat_bubble_agent_shape_for_tail
                )
            } else {
                ContextCompat.getDrawable(
                    itemView.context,
                    R.drawable.chat_bubble_agent_shape
                )
            }
            binding.backgroundMessageBody.setImageDrawable(drawable)
            when {
                message?.previousDirection == Direction.SYSTEM || message?.dateChanged == true -> {
                    binding.topSpace.layoutParams.height = 24.dp
                }
                message?.previousDirection == Direction.INCOMING -> {
                    binding.topSpace.layoutParams.height = 2.dp
                }
                message?.direction == Direction.OUTGOING -> {
                    binding.topSpace.layoutParams.height = 16.dp
                }
                else -> {
                    binding.topSpace.layoutParams.height = 24.dp
                }
            }
            binding.messageBody.setTextColor(attachmentInfoColor)
            binding.messageBody.setOnClickListener(
                when (message?.sendStatus) {
                    SendStatus.SENT -> {
                        attachmentOnClickListener
                    }
                    else -> {
                        null
                    }
                }
            )
        }
    }

    class SystemViewHolder(private val binding: ChatSystemItemBinding) : ViewHolder(binding) {
        override fun bindData() {
            if (message?.dateChanged == true) {
                binding.chatDateLayout.root.isVisible = true
                binding.chatDateLayout.chatCreatedDateTextView.text =
                    message?.sinceDate(itemView.context)
            } else {
                binding.chatDateLayout.root.isVisible = false
            }

            when {
                message?.previousDirection == Direction.INCOMING
                        || message?.previousDirection == Direction.OUTGOING
                        || message?.dateChanged == true  -> {
                    binding.topSpace.layoutParams.height = 24.dp
                }
                else -> {
                    binding.topSpace.layoutParams.height = 16.dp
                }
            }
            binding.txtStartMessage.text = message?.body
        }
    }

    class JoinLeftViewHolder(private val binding: ChatJoinLeftItemBinding) : ViewHolder(binding) {
        override fun bindData() {

            if (message?.dateChanged == true) {
                binding.chatDateLayout.root.isVisible = true
                binding.chatDateLayout.chatCreatedDateTextView.text =
                    message?.sinceDate(itemView.context)
            } else {
                binding.chatDateLayout.root.isVisible = false
            }

            when {
                message?.previousDirection == Direction.INCOMING
                        || message?.previousDirection == Direction.OUTGOING
                        || message?.dateChanged == true -> {
                    binding.topSpace.layoutParams.height = 24.dp
                }
                else -> {
                    binding.topSpace.layoutParams.height = 16.dp
                }
            }
            binding.txtAgentStatus.text = message?.body
            binding.txtAgentTime.text = getTimeFromDateForChat(
                Date(message?.epochDateCreated ?: 0),
                CHAT_HEADER_FORMAT
            )

            setUpRating(
                message?.sid ?: "",
                message?.experienceData,
                onRateExperience
            )
        }
        private fun setUpRating(
            messageSid: String,
            supportExperienceData: ExperienceData?,
            chatAdapterListener: (String, Int, Int) -> Unit
        ) {
            val supportExperienceId = supportExperienceData?.experienceId ?: 0
            val supportExperienceRating = supportExperienceData?.experienceRating ?: 0
            binding.rateChatContainer.isVisible = supportExperienceId != 0
            // Check if Message Has Experience Id to show rating flow
            if (supportExperienceId == 0) {
                return
            }
            if (supportExperienceRating == NOT_RATED) {
                binding.rateUp.setOnClickListener {
                    onRated(UP_RATED, true)
                    chatAdapterListener(messageSid, supportExperienceId, 1)
                }
                binding.rateDown.setOnClickListener {
                    onRated(DOWN_RATED, true)
                    chatAdapterListener(messageSid, supportExperienceId, -1)
                }
            }
            onRated(supportExperienceRating, false)
        }

        private fun onRated(userRating: Int, withAnimation: Boolean) {
            val context = itemView.context
            binding.rateUp.isEnabled = userRating == NOT_RATED
            binding.rateDown.isEnabled = userRating == NOT_RATED
            binding.rateDown.isVisible = userRating != UP_RATED
            binding.rateDownLabel.isVisible = userRating != UP_RATED
            binding.rateUp.isVisible = userRating != DOWN_RATED
            binding.rateUpLabel.isVisible = userRating != DOWN_RATED
            val colorWhite = ContextCompat.getColor(context, R.color.white)
            val colorRed = ContextCompat.getColor(context, R.color.redColor)
            val colorGreen = ContextCompat.getColor(context, R.color.colorGreen)
            val scale = 1.2f
            when (userRating) {
                UP_RATED -> {
                    if (withAnimation) {
                        binding.rateUp.rateClickAnimation(
                            scale,
                            colorWhite,
                            colorGreen
                        )
                    } else {
                        DrawableCompat.setTint(binding.rateUp.background, colorGreen)
                    }
                    binding.rateUp.icon = ContextCompat.getDrawable(context, R.drawable.ic_thumbsup_active)
                    binding.rateUp.updateMarginsRelative(end = 0)
                    binding.rateUpLabel.setText(R.string.thanks_feedback)
                }
                DOWN_RATED -> {
                    if (withAnimation) {
                        binding.rateDown.rateClickAnimation(
                            scale,
                            colorWhite,
                            colorRed
                        )
                    } else {
                        DrawableCompat.setTint(binding.rateDown.background, colorRed)
                    }
                    binding.rateDown.icon = ContextCompat.getDrawable(context, R.drawable.ic_thumbsdown_active)
                    binding.rateDown.updateMarginsRelative(start = 0)
                    binding.rateDownLabel.setText(R.string.thanks_feedback)
                }
                else -> {
                    resetRateButtonUi(binding.rateUp)
                    binding.rateUp.updateMarginsRelative(end = 8.dp)
                    binding.rateUp.icon = ContextCompat.getDrawable(context, R.drawable.ic_thumbsup_inactive)
                    binding.rateDownLabel.isVisible = true
                    binding.rateUpLabel.setText(R.string.excellent)
                    resetRateButtonUi(binding.rateDown)
                    binding.rateDown.updateMarginsRelative(start = 8.dp)
                    binding.rateDown.icon = ContextCompat.getDrawable(context, R.drawable.ic_thumbsdown_inactive)
                    binding.rateDownLabel.isVisible = true
                    binding.rateDownLabel.setText(R.string.bad)
                }
            }
        }

        private fun resetRateButtonUi(view: MaterialButton) {
            DrawableCompat.setTintList(view.background, null)
            view.isVisible = true
        }
    }

    companion object {

        const val UP_RATED: Int = 1
        const val NOT_RATED: Int = 0
        const val DOWN_RATED: Int = -1

        val MESSAGE_COMPARATOR = object : DiffUtil.ItemCallback<MessageDataItem>() {
            override fun areContentsTheSame(
                oldItem: MessageDataItem,
                newItem: MessageDataItem
            ) =
                oldItem == newItem

            override fun areItemsTheSame(
                oldItem: MessageDataItem,
                newItem: MessageDataItem
            ) =
                oldItem.sid == newItem.sid
        }
    }
}
