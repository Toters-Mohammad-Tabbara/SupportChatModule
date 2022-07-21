package com.toters.twilio_chat_module.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.toters.twilio_chat_module._common.SingleLiveEvent
import com.toters.twilio_chat_module.enums.ConversationsError
import com.toters.twilio_chat_module.extensions.ConversationsException
import com.toters.twilio_chat_module.manager.ConnectivityMonitor
import com.toters.twilio_chat_module.manager.LoginManager
import kotlinx.coroutines.launch
import timber.log.Timber

class LoginViewModel(
    private val loginManager: LoginManager,
    connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    val isLoading = MutableLiveData(false)

    private val isNetworkAvailable = connectivityMonitor.isNetworkAvailable.asLiveData(viewModelScope.coroutineContext)

    val onSignInError = SingleLiveEvent<ConversationsError>()

    val onSignInSuccess = SingleLiveEvent<Unit>()

    fun signIn(fetchToken: suspend () -> String) {
        if (isLoading.value == true) return
        Timber.d("signIn in viewModel")

        if (isNetworkAvailable.value == false) {
            Timber.d("no internet connection")
            onSignInError.value = ConversationsError.NO_INTERNET_CONNECTION
            return
        }

        Timber.d("credentials are valid")
        isLoading.value = true
        viewModelScope.launch {
            try {
                loginManager.signIn(fetchToken)
                onSignInSuccess.call()
            } catch (e: ConversationsException) {
                isLoading.value = false
                onSignInError.value = e.error
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            loginManager.signOut()
        }
    }
}
