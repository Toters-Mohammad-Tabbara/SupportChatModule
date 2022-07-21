/*
 * Copyright 2019 Supermac
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.toters.twilio_chat_module.widgets

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.ViewCompat.animate
import com.toters.twilio_chat_module.databinding.ChatLoadingWithTextBinding
import java.util.*

class ChatLoadingWithText: FrameLayout {
    var view: ChatLoadingWithTextBinding =
        ChatLoadingWithTextBinding.inflate(LayoutInflater.from(context), this, true)
    companion object {
        const val period = 250L
    }

    var text: String = ""
    set(value) {
        view.loadingTextView.text = value
        field = value
    }
    val mHandler: Handler = Handler(Looper.getMainLooper())

    constructor(context: Context) : super(context) {
        initLayout()
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initLayout()
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initLayout()
    }

    private fun initLayout()= with(view) {
        loadingTextView.text = text
        startLoading(view)
    }

    fun setTextFont(englishSize:Int,arabicSize:Int,englishFont:String,arabicFont:String)= with(view){
        loadingTextView.setArabicFontSize(arabicSize)
        loadingTextView.setEnglishFontSize(englishSize)
        loadingTextView.setArabicFont(arabicFont)
        loadingTextView.setEnglishFont(englishFont)
    }

    private fun startLoading(view: ChatLoadingWithTextBinding) {
        val timer = Timer()
        timer.schedule(UpdateView(view, mHandler), 0, period)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mHandler.removeCallbacksAndMessages(null)
    }

    internal class UpdateView(val view: ChatLoadingWithTextBinding, val handler: Handler) : TimerTask() {

        private var counter = 0
        private val scaleUpBy = 0.7f
        private val scaleDownBy = -0.6f

        override fun run() {
            when(counter){
                0 -> {
                    handler.post {
                        animate(view.circleOne)
                                .scaleX(1f + scaleUpBy)
                                .scaleY(1f + scaleUpBy)
                                .setDuration(2 * period)
                                .setListener(null).start()
                        animate(view.circleThree)
                                .scaleX(1f + scaleDownBy)
                                .scaleY(1f + scaleDownBy)
                                .setDuration(2 * period)
                                .setListener(null).start()
                    }
                }
                1 -> {
                    handler.post {
                        animate(view.circleTwo)
                                .scaleX(1f + scaleUpBy)
                                .scaleY(1f + scaleUpBy)
                                .setDuration(2 * period)
                                .setListener(null).start()
                        animate(view.circleOne)
                                .scaleX(1f + scaleDownBy)
                                .scaleY(1f + scaleDownBy)
                                .setDuration(2 * period)
                                .setListener(null).start()
                    }
                }
                2 -> {
                    handler.post {
                        animate(view.circleThree)
                                .scaleX(1f + scaleUpBy)
                                .scaleY(1f + scaleUpBy)
                                .setDuration(2 * period)
                                .setListener(null).start()
                        animate(view.circleTwo)
                                .scaleX(1f + scaleDownBy)
                                .scaleY(1f + scaleDownBy)
                                .setDuration(2 * period)
                                .setListener(null).start()
                    }
                }
            }
            counter = (counter + 1) % 3
        }
    }
}