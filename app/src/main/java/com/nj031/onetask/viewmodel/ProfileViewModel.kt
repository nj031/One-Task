package com.nj031.onetask.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import com.nj031.onetask.data.auth.AuthRepository
import com.nj031.onetask.data.profile.Gender
import com.nj031.onetask.data.profile.UserProfile
import com.nj031.onetask.data.profile.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared between ProfileScreen and EditProfileScreen (hoisted in NavGraph, same pattern as
 * HomeViewModel/JournalViewModel) so a save in Edit Profile is immediately reflected back on
 * Profile without a separate reload step.
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserProfileRepository(application)

    val userEmail: String = AuthRepository.currentUser?.email.orEmpty()
    val appVersion: String = readAppVersion(application)

    private val _profile = MutableStateFlow(
        repository.getProfile(defaultName = AuthRepository.currentUser?.displayName.orEmpty())
    )
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    fun saveProfile(name: String, dateOfBirth: Long?, gender: Gender?, photoPath: String?) {
        val updated = UserProfile(name = name, dateOfBirth = dateOfBirth, gender = gender, photoPath = photoPath)
        repository.saveProfile(updated)
        _profile.value = updated
    }

    private fun readAppVersion(application: Application): String = try {
        application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: "unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "unknown"
    }
}
