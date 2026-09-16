package com.nj031.onetask.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nj031.onetask.R
import com.nj031.onetask.data.profile.Gender
import com.nj031.onetask.data.profile.ProfilePhotoStorage
import com.nj031.onetask.viewmodel.ProfileViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    var pickedPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDiscardConfirm by rememberSaveable { mutableStateOf(false) }

    val hasUnsavedChanges = name != initialProfile.name ||
        dateOfBirth != initialProfile.dateOfBirth ||
        genderName != initialProfile.gender?.name ||
        photoRemoved ||
        pickedPhotoUri != null

    val handleBack = {
        if (hasUnsavedChanges) showDiscardConfirm = true else onDone()
    }

    BackHandler(onBack = handleBack)

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pickedPhotoUri = uri
            photoRemoved = false
        }
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
                    pickedPhotoUri != null -> ProfilePhotoStorage.saveFrom(context, pickedPhotoUri!!)
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
                    pickedUri = if (photoRemoved) null else pickedPhotoUri
                )
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    TextButton(onClick = { photoLauncher.launch("image/*") }) {
                        Text(text = stringResource(id = R.string.profile_edit_change_photo))
                    }
                    if (!photoRemoved && (pickedPhotoUri != null || initialProfile.photoPath != null)) {
                        TextButton(onClick = {
                            photoRemoved = true
                            pickedPhotoUri = null
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
                OutlinedTextField(
                    value = dateOfBirth?.let { formatDate(it) }.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(text = stringResource(id = R.string.profile_edit_dob_placeholder)) },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker = true }
                )
                if (dateOfBirth != null) {
                    TextButton(onClick = { dateOfBirth = null }) {
                        Text(text = stringResource(id = R.string.profile_edit_clear))
                    }
                } else {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(text = stringResource(id = R.string.profile_edit_select))
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
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateOfBirth)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateOfBirth = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text(text = stringResource(id = R.string.profile_edit_dob_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
private fun EditablePhoto(existingPhotoPath: String?, pickedUri: Uri?) {
    val previewBitmap = rememberPickedPhotoBitmap(pickedUri)
    val savedBitmap = remember(existingPhotoPath) {
        existingPhotoPath?.let { path -> BitmapFactory.decodeFile(path)?.asImageBitmap() }
    }
    val bitmap = previewBitmap ?: savedBitmap

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
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

@Composable
private fun rememberPickedPhotoBitmap(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uri) {
        bitmap = if (uri == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }
    return bitmap
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

private fun formatDate(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of("UTC")).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
}
