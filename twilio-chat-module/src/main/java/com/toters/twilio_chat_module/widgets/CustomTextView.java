package com.toters.twilio_chat_module.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.Nullable;

import com.google.android.material.textview.MaterialTextView;
import com.toters.twilio_chat_module.R;
import com.toters.twilio_chat_module.widgets.utils.FontCache;
import com.toters.twilio_chat_module.widgets.utils.FontConstants;

/**
 * Created by SuperMAC on 8/29/18.
 */
public class CustomTextView extends MaterialTextView {

    public int englishFontSize;
    public int arabicFontSize;
    public int kurdishFontSize;
    public float textKerning;
    public String englishFont = "";
    public String arabicFont = "";
    public String kurdishFont = "";

    public void setEnglishFont(String englishFont) {
        this.englishFont = englishFont;
        applyCustomStyle();
    }

    public void setArabicFont(String arabicFont) {
        this.arabicFont = arabicFont;
        applyCustomStyle();
    }

    public CustomTextView(Context context) {
        super(context);
        applyCustomStyle();
    }

    public CustomTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.CustomTextView);
        if (a != null) {
            englishFontSize = a.getInt(R.styleable.CustomTextView_englishFontSize, 19);
            arabicFontSize = a.getInt(R.styleable.CustomTextView_arabicFontSize, 23);
            kurdishFontSize = a.getInt(R.styleable.CustomTextView_englishFontSize, 19);
            englishFont = a.getString(R.styleable.CustomTextView_englishFont);
            if (TextUtils.isEmpty(englishFont)) {
                englishFont = FontConstants.NOTO_SANS_SEMI_BOLD;
            }
            arabicFont = a.getString(R.styleable.CustomTextView_arabicFont);
            if (TextUtils.isEmpty(arabicFont)) {
                arabicFont = FontConstants.NOTO_SANS_ARABIC_SEMI_BOLD;
            }
            kurdishFont = a.getString(R.styleable.CustomTextView_kurdishFont);
            if (TextUtils.isEmpty(kurdishFont)) {
                kurdishFont = FontConstants.KURDISH_regular;
            }
            textKerning = a.getFloat(R.styleable.CustomTextView_kerning,0f);
            applyCustomStyle();
            a.recycle();
        }
        // Just by setting a scroll bar to CustomTextView it will be come scrollable
        if (isVerticalScrollBarEnabled() || isHorizontalScrollBarEnabled()) {
            setMovementMethod(new ScrollingMovementMethod());
        }
    }


    public CustomTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        applyCustomStyle();
    }


    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
    }


    private void setArabicFontPath() {
        setTypeface(FontCache.getTypeface("fonts/" + arabicFont, getContext()));
        setTextSize(TypedValue.COMPLEX_UNIT_SP, arabicFontSize);
    }

    private void setEnglishFontPath() {
        setTypeface(FontCache.getTypeface("fonts/" + englishFont, getContext()));
        setTextSize(TypedValue.COMPLEX_UNIT_SP, englishFontSize);
        setLetterSpacing(textKerning);
    }

    private void setKurdishMediumFontToEnglishSemiBold(){
        if (englishFont.equals("noto_sans_semi_bold.ttf")) {
            setTypeface(FontCache.getTypeface("fonts/kurdi_semi_bold.ttf", getContext()));
        }
    }

    private void setKurdishRegularFontToEnglishRegular(){
        if (englishFont.equals("noto_sans_regular")) {
            setTypeface(FontCache.getTypeface("fonts/kurdi_regular.ttf", getContext()));
        }
    }

    private void setKurdishFontPath(){
        setTypeface(FontCache.getTypeface("fonts/" + kurdishFont, getContext()));
        setTextSize(TypedValue.COMPLEX_UNIT_SP, arabicFontSize);
    }

    private void applyCustomStyle(){
        setEnglishFontPath();
//        if (LocaleHelper.getPersistedData(getContext(), "en").equals("en")) {
//            setEnglishFontPath();
//        } else {
//            setArabicFontPath();
//            setKurdishFontPath();
//            setKurdishMediumFontToEnglishSemiBold();
//            setKurdishRegularFontToEnglishRegular();
//        }
    }

    public void setArabicFontSize(int fontSize){
        setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
    }

    public void setEnglishFontSize(int fontSize){
        setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
    }

}
