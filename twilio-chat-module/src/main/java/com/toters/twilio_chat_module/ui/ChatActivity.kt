package com.toters.twilio_chat_module.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.toters.twilio_chat_module.ConversationsClientWrapper
import com.toters.twilio_chat_module.R
import com.toters.twilio_chat_module._initilizer.ConversationConfigs
import com.toters.twilio_chat_module.databinding.ChatMessagesActivityBinding
import com.toters.twilio_chat_module.di.injector
import com.toters.twilio_chat_module.enums.ConversationsError
import com.toters.twilio_chat_module.enums.MessageType
import com.toters.twilio_chat_module.extensions.*
import com.toters.twilio_chat_module.models.MessageListViewItem
import com.toters.twilio_chat_module.ui.dialogs.AttachFileDialog
import com.toters.twilio_chat_module.ui.dialogs.CurvedEdgesErrorDialog
import com.toters.twilio_chat_module.ui.dialogs.ImageShownDialogFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class ChatActivity : BaseActivity() {

    private val supportExperienceId: Int by lazy { intent.getIntExtra(SUPPORT_EXPERIENCE_ID, 0) }

    private val binding by lazy { ChatMessagesActivityBinding.inflate(layoutInflater) }

    private val chatViewModel by lazyViewModel {
        injector.createChatViewModel(intent.getStringExtra(EXTRA_CONVERSATION_SID))
    }

    val messageListViewModel by lazyViewModel {
        injector.createMessageListViewModel(applicationContext, chatViewModel.conversationSid.value)
    }

    private val participantListViewModel by lazyViewModel {
        injector.createParticipantListViewModel(chatViewModel.conversationSid.value)
    }

    private val loginViewModel by lazyViewModel {
        injector.createLoginViewModel(applicationContext)
    }

    var adapter: ChatAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        fetchConversationSid(supportExperienceId)
    }

    private fun fetchConversationSid(supportExperienceId: Int) {
        chatViewModel.fetchConversationSid(supportExperienceId)
        chatViewModel.conversationSid.onEach { conversationSid ->
            if (conversationSid.isNotEmpty()) {
                signInToTwilio()
                isLoading()
                onSignInSuccess()
                onSignInError()
                resentUnSentMessages()
            }
        }.launchIn(lifecycleScope)
    }
    private fun signInToTwilio() {
        if (ConversationsClientWrapper.INSTANCE.isClientCreated.not()) {
            loginViewModel.signIn {
                ConversationsClientWrapper.INSTANCE.chatCallback?.fetchToken() ?: ""
            }
        } else {
            initViews()
        }
    }

    private fun isLoading() {
        loginViewModel.isLoading.observe(this@ChatActivity) { isLoading ->
            if (isLoading) {
                showLoading()
            } else {
                hideLoading()
            }
        }
    }

    private fun onSignInError() {
        loginViewModel.onSignInError.observe(this@ChatActivity) {
            showRoundedEdgesDialog(getString(R.string.error_title),
                it.message,
                getString(R.string.retry),
                getString(R.string.cancel),
                object : CurvedEdgesErrorDialog.CustomDialogInterface {
                    override fun setOnPositiveButtonClick(dialog: Dialog?) {
                        loginViewModel.signIn {
                            ConversationsClientWrapper.INSTANCE.chatCallback?.fetchToken()
                                ?: ""
                        }
                        dialog?.dismiss()
                    }

                    override fun setOnNegativeButtonClick(dialog: Dialog?) {
                        dialog?.dismiss()
                        finish()
                    }
                })
        }
    }

    private fun onSignInSuccess() {
        loginViewModel.onSignInSuccess.observe(this@ChatActivity) {
            initViews()
        }
    }

    private fun resentUnSentMessages() {
        messageListViewModel.getUnsentMessages()
        messageListViewModel.unsentMessages.observe(this@ChatActivity) { messages ->
            messages?.forEach { message ->
                resendMessage(
                    message.toMessageListViewItem(
                        0,
                        authorChanged = false,
                        dateChanged = false
                    )
                )
            }
        }
    }

    private fun showLoading() {
        binding.loadingIndicator.isVisible = true
        binding.loadingImage.isVisible = true
        binding.loadingImage.setImageResource(R.drawable.customer_support_200dp)
        binding.loadingIndicator.text = getString(R.string.connecting)
    }

    private fun hideLoading() {
        binding.loadingIndicator.isVisible = false
        binding.loadingImage.isVisible = false
    }

    private fun initViews() {
        hideLoading()
        adapter = ChatAdapter(
            onDisplaySendError = { message ->
                Timber.d("Display send error clicked: ${message.uuid}")
                showSendErrorDialog(message)
            },
            openMessageMedia = { message ->
                Timber.d("Download clicked: $message")
                messageListViewModel.openMessageMedia(
                    message.index
                )
            },
            onRateExperience = { messageSid, experienceId, rating ->
                chatViewModel.rateExperience(messageSid, experienceId, rating)
            }
        )

        binding.chatRecyclerView.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        binding.chatRecyclerView.layoutManager = linearLayoutManager

        binding.chatRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val index = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                if (index == RecyclerView.NO_POSITION) return

                val message = adapter?.getMessage(index)
                message?.let { messageListViewModel.handleMessageDisplayed(it.index) }
            }
        })
        binding.messagingContainer.isVisible = ConversationConfigs.featureFlags.canInitiateChat
        binding.messageEditText.onSubmit {
            sendMessage()
        }
        binding.messageEditText.setButtonColorOnTextState(
            binding.sendButton,
            R.drawable.ic_send_message_disabled,
            R.drawable.ic_send_message
        )

        binding.sendButton.setOnClickListener {
            sendMessage()
        }
        binding.messageEditText.doAfterTextChanged {
            messageListViewModel.typing()
        }
        messageListViewModel.messageItems.observe(this) { messages ->
            adapter?.submitData(lifecycle, messages)
            adapter?.onPagesUpdatedFlow?.onEach {
                binding.chatRecyclerView.scrollToPosition((adapter?.itemCount ?: 0) - 1)
            }?.launchIn(lifecycleScope)
        }

        messageListViewModel.openMedia.observe(this) {
            viewUri(it)
        }

        messageListViewModel.onMessageCopied.observe(this) {
            binding.messageEditText.showSnackbar(R.string.message_copied, R.id.messageEditText)
        }
        messageListViewModel.onMessageError.observe(this) { error ->
            if (error == ConversationsError.CONVERSATION_GET_FAILED) {
                finish()
                return@observe
            }
            if (error == ConversationsError.MESSAGE_SEND_FAILED) { // shown in message list inline
                return@observe
            }
            binding.root.showSnackbar(getErrorMessage(error), R.id.messageEditText)
        }
        messageListViewModel.typingParticipantsList.observe(this) { participants ->
            binding.typingIndicator.isVisible = participants.isNotEmpty()
            if (participants.isNotEmpty()) {
                binding.typingIndicator.text =
                    resources.getString(R.string.typing_indicator, participants[0])
            }
        }
        binding.attachImageButton.setOnClickListener {
            AttachFileDialog.getInstance(messageListViewModel.conversationSid)
                .showNow(supportFragmentManager, null)
        }
    }

    private fun showSendErrorDialog(message: MessageListViewItem) {
        val title = getString(R.string.send_error_dialog_title, message.errorCode)

        val text = when (message.errorCode) { // See https://www.twilio.com/docs/api/errors
            50511 -> getString(R.string.send_error_dialog_invalid_media_content_type)
            else -> getString(R.string.send_error_dialog_message_default)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(text)
            .setPositiveButton(R.string.close, null)
            .setNegativeButton(R.string.retry) { _, _ -> resendMessage(message) }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isAllCaps = false
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isAllCaps = false
        }

        dialog.show()
    }

    private fun sendMessage() {
        if (participantListViewModel.agentInChat.value != true) {
            chatViewModel.reInitiateChat()
        }
        binding.messageEditText.text.toString().takeIf { it.isNotBlank() }?.let { message ->
            Timber.d("Sending message: $message")
            messageListViewModel.sendTextMessage(message)
            binding.messageEditText.text?.clear()
        }
    }

    private fun resendMessage(message: MessageListViewItem) {
        if (participantListViewModel.agentInChat.value != true) {
            chatViewModel.reInitiateChat()
        }
        if (message.type == MessageType.TEXT) {
            messageListViewModel.resendTextMessage(message.uuid)
        } else if (message.type == MessageType.MEDIA) {
            val fileInputStream = message.mediaUploadUri?.let { contentResolver.openInputStream(it) }
            if (fileInputStream != null) {
                messageListViewModel.resendMediaMessage(fileInputStream, message.uuid)
            } else {
                Timber.w("Could not get input stream for file reading: ${message.mediaUploadUri}")
                showToast(R.string.err_failed_to_resend_media)
            }
        }
    }

    private fun viewUri(imageUrl: Uri) {
        val dialogFragment= ImageShownDialogFragment.newInstance(imageUrl.toString())
        dialogFragment.show(supportFragmentManager,"")
    }

    companion object {

        private const val EXTRA_CONVERSATION_SID = "ExtraConversationSid"
        private const val SUPPORT_EXPERIENCE_ID = "SupportExperienceId"

        @JvmOverloads
        fun start(context: Context, conversationSid: String, supportExperienceId: Int = 0) =
            context.startActivity(getStartIntent(context, conversationSid,supportExperienceId))

        @JvmOverloads
        fun getStartIntent(
            context: Context,
            conversationSid: String? = ConversationConfigs.conversationSid,
            supportExperienceId: Int = 0
        ) =
            Intent(context, ChatActivity::class.java)
                .putExtra(EXTRA_CONVERSATION_SID, conversationSid)
                .putExtra(SUPPORT_EXPERIENCE_ID, supportExperienceId)
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        super.onBackPressed()
        if (isTaskRoot) {
            ConversationsClientWrapper.INSTANCE.chatCallback?.onBackPressed(this)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        ConversationConfigs.isChatActivityOpen = true
    }

    override fun onPause() {
        super.onPause()
        ConversationConfigs.isChatActivityOpen = false
    }
}
