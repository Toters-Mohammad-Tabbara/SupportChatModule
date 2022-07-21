package com.toters.twilio_chat_module.extensions

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView


fun EditText.setButtonColorOnTextState(imageButton: ImageView, offStateRes: Int, onStateRes: Int) {
    addTextChangedListener( object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {}

        override fun beforeTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
            if(text?.trim()?.length ?: 0 == 0) {
                imageButton.setImageResource(offStateRes)
                imageButton.isEnabled = false
            } else {
                imageButton.setImageResource(onStateRes)
                imageButton.isEnabled = true
            }
        }

    })
}