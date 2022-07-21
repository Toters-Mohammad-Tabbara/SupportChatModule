package com.toters.twilio_chat_module.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.google.android.material.button.MaterialButton;
import com.toters.twilio_chat_module.R;
import com.toters.twilio_chat_module.widgets.utils.FontCache;
import com.toters.twilio_chat_module.widgets.utils.FontConstants;


/**
 * Created by SuperMAC on 8/29/18.
 */
public class CustomMaterialButton extends MaterialButton {

    public String englishFont = "";
    public String arabicFont = "";
    public int englishFontSize;
    public int arabicFontSize;
    public float letterSpacing;

    public CustomMaterialButton(Context context) {
        super(context);
        applyCustomStyle();
    }

    public CustomMaterialButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomMaterialButton);
        englishFontSize = a.getInt(R.styleable.CustomMaterialButton_btnEnglishFontSize, 12);
        arabicFontSize = a.getInt(R.styleable.CustomMaterialButton_btnArabicFontSize, 12);
        englishFont = a.getString(R.styleable.CustomMaterialButton_btnEnglishFont);
        if (TextUtils.isEmpty(englishFont)) {
            englishFont = FontConstants.NOTO_SANS_SEMI_BOLD;
        }
        arabicFont = a.getString(R.styleable.CustomMaterialButton_btnArabicFont);
        if (TextUtils.isEmpty(arabicFont)) {
            arabicFont = FontConstants.NOTO_SANS_ARABIC_SEMI_BOLD;
        }
        letterSpacing = a.getFloat(R.styleable.CustomMaterialButton_btnLetterSpacing, 0.1f);
        applyCustomStyle();
        a.recycle();
    }

    public CustomMaterialButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        applyCustomStyle();
    }

    private void setArabicTextStyle() {
        setTypeface(FontCache.getTypeface("fonts/" + arabicFont, getContext()));
        setTextSize(TypedValue.COMPLEX_UNIT_SP, arabicFontSize);
    }

    private void setEnglishTextStyle() {
        setTypeface(FontCache.getTypeface("fonts/" + englishFont, getContext()));
        setTextSize(TypedValue.COMPLEX_UNIT_SP, englishFontSize);
        setLetterSpacing(letterSpacing);
    }

    private void setKurdishFont(){
        if (englishFont.equals("noto_sans_semi_bold")) {
            setTypeface(FontCache.getTypeface("fonts/kurdi_semi_bold.ttf", getContext()));
        }
        setTextSize(TypedValue.COMPLEX_UNIT_SP, englishFontSize);
    }

    private void applyCustomStyle(){
        setEnglishTextStyle();
//        if (LocaleHelper.isEnglish(getContext())) {
//            setEnglishTextStyle();
//        } else {
//            setArabicTextStyle();
//            setKurdishFont();
//        }
    }
}
