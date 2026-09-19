package com.nj031.onetask.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nj031.onetask.data.auth.AuthRepository
import com.nj031.onetask.data.profile.Gender
import com.nj031.onetask.data.profile.UserProfile
import com.nj031.onetask.data.profile.UserProfileRepository
import com.nj031.onetask.data.sync.CloudBackupRepository
import com.nj031.onetask.data.sync.CloudProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

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

    /** [photoChanged] is true only when this save actually replaced or removed the photo (set by
     * EditProfileScreen, which is the only place with that knowledge - a plain photoPath == null
     * can't distinguish "never had one" from "just removed it"). Only then does this also sync
     * the photo to/from Firebase Storage - every other save (name/DOB/gender only) leaves the
     * cloud photo untouched. */
    fun saveProfile(name: String, dateOfBirth: Long?, gender: Gender?, photoPath: String?, photoChanged: Boolean = false) {
        val updated = UserProfile(name = name, dateOfBirth = dateOfBirth, gender = gender, photoPath = photoPath)
        repository.saveProfile(updated)
        _profile.value = updated
        pushToCloud(photoChanged, photoPath)
    }

    /** Account-owned - see the "Future Account-Persistence Architecture Rule": Profile must
     * survive uninstall/reinstall for the same account. The local SharedPreferences write above
     * already happened synchronously (driving the UI instantly); this only pushes the durable
     * cloud copy (Firestore for name/DOB/gender, Firebase Storage for the photo when it
     * changed), off the calling thread. */
    private fun pushToCloud(photoChanged: Boolean, photoPath: String?) {
        viewModelScope.launch {
            val current = repository.getProfile(defaultName = AuthRepository.currentUser?.displayName.orEmpty())
            CloudBackupRepository.pushProfile(
                CloudProfile(
                    name = current.name,
                    dateOfBirth = current.dateOfBirth,
                    gender = current.gender?.name,
                    updatedAt = repository.getUpdatedAt()
                )
            )
            if (photoChanged) {
                if (photoPath != null) {
                    CloudBackupRepository.uploadProfilePhoto(File(photoPath))
                } else {
                    CloudBackupRepository.deleteProfilePhoto()
                }
            }
        }
    }

    private fun readAppVersion(application: Application): String = try {
        application.packageManager.getPackageInfo(application.packageName, 0).versionName ?: "unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "unknown"
    }
}
