/*
 *  Copyright (c) 2018. SuperMAC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * imitations under the License.
 *
 */
package com.toters.twilio_chat_module.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import com.toters.twilio_chat_module.R
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.toters.twilio_chat_module.databinding.ErrorDialogLayoutBinding
import com.toters.twilio_chat_module.widgets.utils.FontCache
import com.toters.twilio_chat_module.widgets.utils.FontConstants

/**
 * Created by SuperMAC on 9/25/18.
 */
class CurvedEdgesErrorDialog(
    context: Context,
    private val titleTxt: String?,
    private val bodyTxt: String?,
    private val positiveBtnTxt: String?,
    private val negativeBtnTxt: String?,
    private val dialogInterface: CustomDialogInterface?
) : Dialog(context) {

    interface CustomDialogInterface {
        fun setOnPositiveButtonClick(dialog: Dialog?)
        fun setOnNegativeButtonClick(dialog: Dialog?)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ErrorDialogLayoutBinding.inflate(
            LayoutInflater.from(
                context
            )
        )
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCanceledOnTouchOutside(false)
        setContentView(binding.root)
        binding.dialogTitle.text = titleTxt
        binding.dialogTitle.textAlignment = View.TEXT_ALIGNMENT_CENTER
        binding.dialogTitle.visibility = View.VISIBLE
        binding.dialogTitle.setTextColor(ContextCompat.getColor(context, R.color.black))
        binding.dialogTitle.typeface = FontCache.getTypeface(
            "fonts/" + FontConstants.NOTO_SANS_SEMI_BOLD,
            context
        )
        binding.dialogMsg.text = bodyTxt
        binding.dialogMsg.textAlignment = View.TEXT_ALIGNMENT_CENTER
        binding.dialogMsg.isVisible = !bodyTxt.isNullOrEmpty()

        binding.dialogPositiveBtn.text = positiveBtnTxt
        binding.dialogPositiveBtn.isVisible = !TextUtils.isEmpty(positiveBtnTxt)
        binding.dialogPositiveBtn.setTextColor(
            ContextCompat.getColor(
                context,
                R.color.colorGreen
            )
        )
        binding.dialogNegativeBtn.text = negativeBtnTxt
        binding.dialogNegativeBtn.setTextColor(
            ContextCompat.getColor(
                context,
                R.color.colorGreen
            )
        )
        binding.dialogNegativeBtn.isVisible = !TextUtils.isEmpty(positiveBtnTxt)
        binding.dialogPositiveBtn.setOnClickListener {
            dialogInterface?.setOnPositiveButtonClick(this)
        }

        binding.dialogNegativeBtn.isVisible = !TextUtils.isEmpty(negativeBtnTxt)
        binding.dialogNegativeBtn.setOnClickListener {
            dialogInterface?.setOnNegativeButtonClick(this)
        }

        binding.buttonSeparator.isVisible = !TextUtils.isEmpty(negativeBtnTxt) || !TextUtils.isEmpty(positiveBtnTxt)
        binding.dialogTitle.isVisible = titleTxt != null
    }
}