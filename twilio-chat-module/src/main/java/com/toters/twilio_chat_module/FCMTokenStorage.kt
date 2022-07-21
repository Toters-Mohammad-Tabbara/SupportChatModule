package com.toters.twilio_chat_module

import android.content.Context
import androidx.preference.PreferenceManager
import timber.log.Timber
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class FCMTokenStorage constructor(applicationContext: Context) {

    var fcmToken by stringPreference()

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

    private fun stringPreference() = object : ReadWriteProperty<Any?, String> {

        override fun getValue(thisRef: Any?, property: KProperty<*>): String {
            Timber.d("CredentialStorage getValue()")
            return sharedPreferences.getString(property.name, "")!!
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            Timber.d("CredentialStorage setValue()")
            sharedPreferences.edit()
                .putString(property.name, value)
                .apply()
        }
    }

    fun clearCredentials() {
        Timber.d("clearCredentials")
        sharedPreferences.edit().clear().apply()
    }
}
