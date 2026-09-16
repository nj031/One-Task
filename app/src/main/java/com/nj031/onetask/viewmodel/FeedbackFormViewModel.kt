package com.nj031.onetask.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nj031.onetask.R
import com.nj031.onetask.data.feedback.FeedbackRepository
import com.nj031.onetask.data.feedback.FeedbackType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FeedbackSubmitState {
    data object Idle : FeedbackSubmitState()
    data object Submitting : FeedbackSubmitState()
    data object Success : FeedbackSubmitState()
    data class Error(val message: String) : FeedbackSubmitState()
}

/**
 * Backs Suggest a Feature / Report a Problem / Send Feedback - identical submit/loading/retry
 * behavior for all three, differing only in the [FeedbackType] passed to [submit] and, for bug
 * reports, the device diagnostics automatically attached alongside the user's message.
 */
class FeedbackFormViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<FeedbackSubmitState>(FeedbackSubmitState.Idle)
    val uiState: StateFlow<FeedbackSubmitState> = _uiState.asStateFlow()

    fun submit(type: FeedbackType, message: String) {
        if (_uiState.value == FeedbackSubmitState.Submitting) return
        viewModelScope.launch {
            _uiState.value = FeedbackSubmitState.Submitting
            try {
                FeedbackRepository.submit(
                    type = type,
                    message = message,
                    appVersion = appVersionName(),
                    androidVersion = if (type == FeedbackType.BUG) Build.VERSION.RELEASE else null,
                    deviceModel = if (type == FeedbackType.BUG) Build.MODEL else null
                )
                _uiState.value = FeedbackSubmitState.Success
            } catch (e: Exception) {
                _uiState.value = FeedbackSubmitState.Error(
                    e.message ?: getApplication<Application>().getString(R.string.feedback_form_generic_error)
                )
            }
        }
    }

    private fun appVersionName(): String {
        val application = getApplication<Application>()
        return try {
            application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: "unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "unknown"
        }
    }
}
