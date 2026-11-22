package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CharacterProfileEntity
import com.example.data.model.TransitionStyle
import com.example.data.model.VideoSceneEntity
import com.example.localization.AppLanguage
import com.example.localization.StudioStrings

@Composable
fun EditCharacterDialog(
    initial: CharacterProfileEntity?,
    strings: StudioStrings,
    onDismiss: () -> Unit,
    onSave: (name: String, appearance: String, voiceTone: String, imageUri: String?) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var appearance by remember { mutableStateOf(initial?.appearancePrompt.orEmpty()) }
    var voiceTone by remember { mutableStateOf(initial?.voiceTone.orEmpty()) }
    var imageUri by remember { mutableStateOf(initial?.referenceImageUri) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            imageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) strings.addCharacter else initial.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.characterNameHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = appearance,
                    onValueChange = { appearance = it },
                    label = { Text(strings.characterAppearanceHint) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = voiceTone,
                    onValueChange = { voiceTone = it },
                    label = { Text(strings.characterVoiceHint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.pickImagesButton)
                    }
                    if (imageUri != null) {
                        AsyncImage(
                            model = Uri.parse(imageUri),
                            contentDescription = "Character reference",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, appearance, voiceTone, imageUri) },
                enabled = name.isNotBlank()
            ) {
                Text(strings.saveChanges)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditSceneDialog(
    scene: VideoSceneEntity,
    strings: StudioStrings,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (VideoSceneEntity) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(scene.title) }
    var visualPrompt by remember { mutableStateOf(scene.visualPrompt) }
    var narration by remember { mutableStateOf(scene.narrationText) }
    var subtitles by remember { mutableStateOf(scene.subtitleText) }
    var durationSec by remember { mutableIntStateOf(scene.durationSeconds.coerceIn(3, 120)) }
    var transition by remember { mutableStateOf(scene.transitionStyle) }
    var imageUri by remember { mutableStateOf(scene.sourceImageUri) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            imageUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editScene) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = visualPrompt,
                    onValueChange = { visualPrompt = it },
                    label = { Text("Visual / Video Prompt") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = narration,
                    onValueChange = { narration = it },
                    label = { Text("Voice Narration (Hindi / English)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitles,
                    onValueChange = { subtitles = it },
                    label = { Text("Subtitles") },
                    minLines = 1,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "${strings.durationMinutesSeconds(durationSec)}",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = durationSec.toFloat(),
                    onValueChange = { durationSec = it.toInt() },
                    valueRange = 3f..120f
                )

                Text(text = strings.transitionLabel, style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TransitionStyle.entries.forEach { style ->
                        FilterChip(
                            selected = transition == style,
                            onClick = { transition = style },
                            label = {
                                Text(if (appLanguage == AppLanguage.HINDI) style.labelHi else style.labelEn)
                            }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.pickImagesButton)
                    }
                    if (imageUri != null) {
                        AsyncImage(
                            model = Uri.parse(imageUri),
                            contentDescription = "Scene photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        scene.copy(
                            title = title.ifBlank { scene.title },
                            visualPrompt = visualPrompt,
                            narrationText = narration,
                            subtitleText = subtitles,
                            durationSeconds = durationSec,
                            transitionStyle = transition,
                            sourceImageUri = imageUri
                        )
                    )
                }
            ) {
                Text(strings.saveChanges)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
