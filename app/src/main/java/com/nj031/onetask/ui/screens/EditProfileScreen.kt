package com.nj031.onetask.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.profile.Gender
import com.nj031.onetask.data.profile.ProfilePhotoStorage
import com.nj031.onetask.ui.components.OneTaskCalendarSheet
import com.nj031.onetask.viewmodel.ProfileViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-screen profile editor. Name/DOB/Gender/photo edits live entirely in local composable
 * state until Save Changes is tapped - nothing is written to [ProfileViewModel] (and therefore
 * nothing is persisted) until then, so a picked photo or typed name is safely discardable if the
 * user backs out without saving.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialProfile = viewModel.profile.value

    var name by rememberSaveable { mutableStateOf(initialProfile.name) }
    var nameError by rememberSaveable { mutableStateOf(false) }
    var dateOfBirth by rememberSaveable { mutableStateOf(initialProfile.dateOfBirth) }
    var genderName by rememberSaveable { mutableStateOf(initialProfile.gender?.name) }
    var photoRemoved by rememberSaveable { mutableStateOf(false) }
    // The freshly-picked source (if any) that croppedPhotoBitmap was cropped from - kept around so
    // tapping the photo again to re-crop starts from the original full-resolution image rather
    // than re-cropping an already-cropped bitmap. Not rememberSaveable: a content Uri's read
    // permission doesn't reliably survive process death anyway, and croppedPhotoBitmap (which
    // isn't Parcelable-safe to save regardless) would be lost together with it.
    var originalPickedUri by remember { mutableStateOf<Uri?>(null) }
    var croppedPhotoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cropSourceUri by remember { mutableStateOf<Uri?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDiscardConfirm by rememberSaveable { mutableStateOf(false) }

    val hasUnsavedChanges = name != initialProfile.name ||
        dateOfBirth != initialProfile.dateOfBirth ||
        genderName != initialProfile.gender?.name ||
        photoRemoved ||
        croppedPhotoBitmap != null

    val handleBack = {
        if (hasUnsavedChanges) showDiscardConfirm = true else onDone()
    }

    BackHandler(onBack = handleBack)

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            cropSourceUri = uri
        }
    }

    // The existing photo (freshly picked-and-cropped this session, or already saved on disk) that
    // tapping the avatar should re-open the crop step on - null once there's nothing to re-crop,
    // in which case tapping it opens the picker instead.
    val recropSource = originalPickedUri
        ?: initialProfile.photoPath?.takeIf { !photoRemoved }?.let { path -> Uri.fromFile(File(path)) }

    fun onPhotoClick() {
        if (recropSource != null) cropSourceUri = recropSource else photoLauncher.launch("image/*")
    }

    fun onSaveClick() {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            nameError = true
            return
        }
        scope.launch {
            val finalPhotoPath = withContext(Dispatchers.IO) {
                when {
                    photoRemoved -> {
                        ProfilePhotoStorage.delete(context)
                        null
                    }
                    croppedPhotoBitmap != null -> ProfilePhotoStorage.saveBitmap(context, croppedPhotoBitmap!!)
                    else -> initialProfile.photoPath
                }
            }
            viewModel.saveProfile(
                name = trimmedName,
                dateOfBirth = dateOfBirth,
                gender = genderName?.let { raw -> runCatching { Gender.valueOf(raw) }.getOrNull() },
                photoPath = finalPhotoPath
            )
            onDone()
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = handleBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = stringResource(id = R.string.profile_edit_profile),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EditablePhoto(
                    existingPhotoPath = if (photoRemoved) null else initialProfile.photoPath,
                    croppedBitmap = if (photoRemoved) null else croppedPhotoBitmap,
                    onClick = ::onPhotoClick
                )
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    TextButton(onClick = { photoLauncher.launch("image/*") }) {
                        Text(text = stringResource(id = R.string.profile_edit_change_photo))
                    }
                    if (!photoRemoved && (croppedPhotoBitmap != null || initialProfile.photoPath != null)) {
                        TextButton(onClick = {
                            photoRemoved = true
                            croppedPhotoBitmap = null
                            originalPickedUri = null
                        }) {
                            Text(
                                text = stringResource(id = R.string.profile_edit_remove_photo),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = false
                },
                label = { Text(text = stringResource(id = R.string.profile_edit_name_label)) },
                singleLine = true,
                isError = nameError,
                supportingText = {
                    if (nameError) {
                        Text(text = stringResource(id = R.string.profile_edit_name_required))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp)
            )

            Text(
                text = stringResource(id = R.string.profile_edit_dob_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateOfBirth?.let { formatDate(it) } ?: stringResource(id = R.string.profile_edit_dob_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (dateOfBirth != null) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (dateOfBirth != null) {
                    TextButton(onClick = { dateOfBirth = null }) {
                        Text(text = stringResource(id = R.string.profile_edit_clear))
                    }
                }
            }

            Text(
                text = stringResource(id = R.string.profile_edit_gender_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GenderChip(
                    text = stringResource(id = R.string.profile_edit_gender_male),
                    selected = genderName == Gender.MALE.name,
                    onClick = { genderName = if (genderName == Gender.MALE.name) null else Gender.MALE.name }
                )
                GenderChip(
                    text = stringResource(id = R.string.profile_edit_gender_female),
                    selected = genderName == Gender.FEMALE.name,
                    onClick = { genderName = if (genderName == Gender.FEMALE.name) null else Gender.FEMALE.name }
                )
                GenderChip(
                    text = stringResource(id = R.string.profile_edit_gender_prefer_not_to_say),
                    selected = genderName == Gender.PREFER_NOT_TO_SAY.name,
                    onClick = {
                        genderName = if (genderName == Gender.PREFER_NOT_TO_SAY.name) null else Gender.PREFER_NOT_TO_SAY.name
                    }
                )
            }

            Text(
                text = stringResource(id = R.string.profile_edit_email_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            Text(
                text = viewModel.userEmail,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { onSaveClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.profile_edit_save_changes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDatePicker) {
        // Same shared calendar presentation used everywhere else in One Task (Homepage/Journal/
        // Add Task) instead of Material3's own DatePickerDialog, per the app-wide calendar
        // consistency requirement - it already uses the One Task palette and already supports
        // capping selectable dates (maxSelectableDate), which is exactly what "DOB can't be in
        // the future" needs. dateOfBirth itself stays a UTC-epoch-millis Long (unchanged
        // persistence shape); only this picker's own input/output is converted to/from
        // LocalDate at the UI boundary.
        OneTaskCalendarSheet(
            initialDate = dateOfBirth?.let(::epochMillisToUtcLocalDate) ?: LocalDate.now(),
            onDateSelected = { date -> dateOfBirth = date.utcEpochMillis() },
            onDismiss = { showDatePicker = false },
            maxSelectableDate = LocalDate.now()
        )
    }

    cropSourceUri?.let { uri ->
        ProfilePhotoCropDialog(
            sourceUri = uri,
            onCancel = { cropSourceUri = null },
            onDone = { cropped ->
                croppedPhotoBitmap = cropped
                // Only promoted to the re-crop source once a crop is actually applied - a
                // picked-then-cancelled image must not linger as something "tap the avatar" would
                // later try to re-crop instead of opening the picker again.
                originalPickedUri = uri
                photoRemoved = false
                cropSourceUri = null
            }
        )
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text(text = stringResource(id = R.string.profile_edit_discard_title)) },
            text = { Text(text = stringResource(id = R.string.profile_edit_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        onDone()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.profile_edit_discard_action),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text(text = stringResource(id = R.string.profile_edit_keep_editing))
                }
            }
        )
    }
}

@Composable
private fun EditablePhoto(existingPhotoPath: String?, croppedBitmap: Bitmap?, onClick: () -> Unit) {
    val savedBitmap = remember(existingPhotoPath) {
        existingPhotoPath?.let { path -> BitmapFactory.decodeFile(path)?.asImageBitmap() }
    }
    val bitmap = croppedBitmap?.asImageBitmap() ?: savedBitmap

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

/** Same pill-shaped selectable chip style used for Tag/Date/Repeat rows in Add/Edit Task. */
@Composable
private fun GenderChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        },
        shape = RoundedCornerShape(50),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onBackground,
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

private fun formatDate(epochMillis: Long): String =
    epochMillisToUtcLocalDate(epochMillis).format(DateTimeFormatter.ofPattern("d MMM yyyy"))

/** [dateOfBirth] is stored as UTC-epoch millis (its original Material3 DatePicker shape, kept
 * as-is here since only the picker UI changed, not the persisted representation) - these two
 * converters are the sole bridge to/from OneTaskCalendarSheet's LocalDate-based API. */
private fun epochMillisToUtcLocalDate(epochMillis: Long): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of("UTC")).toLocalDate()

private fun LocalDate.utcEpochMillis(): Long =
    atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
