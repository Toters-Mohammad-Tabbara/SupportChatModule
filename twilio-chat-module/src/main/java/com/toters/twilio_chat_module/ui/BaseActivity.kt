package com.toters.twilio_chat_module.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import com.toters.twilio_chat_module.ui.dialogs.CurvedEdgesErrorDialog

private const val TAG_SPLASH_FRAGMENT = "TAG_SPLASH_FRAGMENT"

open class BaseActivity : AppCompatActivity() {

    open fun showRoundedEdgesDialog(
        title: String?, message: String, positiveBtn: String?,
        negativeBtn: String?, customDialogInterface: CurvedEdgesErrorDialog.CustomDialogInterface?
    ) {
        val dialog =
            CurvedEdgesErrorDialog(
                this,
                title, message, positiveBtn, negativeBtn,
                customDialogInterface
            )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        if (!isFinishing && message != "") {
            dialog.show()
        }
    }
}
