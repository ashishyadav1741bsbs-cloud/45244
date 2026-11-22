package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CreationMode
import com.example.data.model.StoryLanguage
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoResolution
import com.example.localization.AppLanguage
import com.example.localization.StudioStrings
import com.example.ui.theme.DirectorAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.viewmodel.StudioDraftState
import com.example.ui.viewmodel.StudioViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioCreateScreen(
    draft: StudioDraftState,
    strings: StudioStrings,
    appLanguage: AppLanguage,
    viewModel: StudioViewModel,
    onProjectCreated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            viewModel.addImages(uris.map { it.toString() })
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_hero),
                        contentDescription = strings.heroHeadline,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x550B0F1A),
                                        Color(0xEE0B0F1A)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = StudioEmerald.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = strings.noCreditsBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioEmerald,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = strings.heroHeadline,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.heroTagline,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD0DCF5),
                            maxLines = 2
                        )
                    }
                }
            }

            // 1. Creation Mode Selector (Text to Video | Image to Video | Script to Video)
            Text(
                text = strings.modeSectionTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ModeOptionCard(
                    title = strings.modeTextToVideo,
                    subtitle = strings.modeTextDesc,
                    icon = Icons.Default.TextFields,
                    selected = draft.mode == CreationMode.TEXT_TO_VIDEO,
                    testTag = "mode_text_to_video",
                    onClick = { viewModel.setMode(CreationMode.TEXT_TO_VIDEO) }
                )
                ModeOptionCard(
                    title = strings.modeImageToVideo,
                    subtitle = strings.modeImageDesc,
                    icon = Icons.Default.Image,
                    selected = draft.mode == CreationMode.IMAGE_TO_VIDEO,
                    testTag = "mode_image_to_video",
                    onClick = { viewModel.setMode(CreationMode.IMAGE_TO_VIDEO) }
                )
                ModeOptionCard(
                    title = strings.modeScriptToVideo,
                    subtitle = strings.modeScriptDesc,
                    icon = Icons.Default.Description,
                    selected = draft.mode == CreationMode.SCRIPT_TO_VIDEO,
                    testTag = "mode_script_to_video",
                    onClick = { viewModel.setMode(CreationMode.SCRIPT_TO_VIDEO) }
                )
            }

            // 2. Story / Script Input Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = draft.title,
                        onValueChange = viewModel::setTitle,
                        label = { Text(strings.projectTitleLabel) },
                        placeholder = { Text(strings.projectTitleHint) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("project_title_input")
                    )

                    OutlinedTextField(
                        value = draft.prompt,
                        onValueChange = viewModel::setPrompt,
                        label = { Text(strings.scriptLabel) },
                        placeholder = { Text(strings.scriptHint) },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("script_prompt_input")
                    )

                    // Beginner Quick Starters
                    Text(
                        text = strings.starterTemplatesLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                viewModel.applyStarterTemplate(
                                    title = "बनारस की सुबह",
                                    prompt = "आरव सुबह-सुबह बनारस के घाट पर पहुँचता है। गंगा नदी पर सुनहरी धूप चमक रही है और नावें धीरे-धीरे चल रही हैं। तभी उसकी पुरानी दोस्त मीरा चाय का कुल्हड़ लेकर आती है। दोनों अपने नए सफर की शुरुआत करते हैं।",
                                    language = StoryLanguage.HINDI
                                )
                            },
                            label = { Text("हिंदी कहानी (Varanasi)") }
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                viewModel.applyStarterTemplate(
                                    title = "Solar Frontier 2045",
                                    prompt = "Elena steps onto the sunlit orbital deck overlooking Earth. The telemetry screen glows amber as the solar sail unfolds into space. Captain Kabir confirms the trajectory is clear for deep-space transit.",
                                    language = StoryLanguage.ENGLISH
                                )
                            },
                            label = { Text("Sci-Fi Story (English)") }
                        )
                    }

                    // Photo Picker for Image-to-Video or Reference Frames
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.testTag("pick_images_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = strings.pickImagesButton,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(strings.pickImagesButton)
                        }

                        if (draft.selectedImageUris.isNotEmpty()) {
                            Text(
                                text = strings.selectedImagesCount(draft.selectedImageUris.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                        }
                    }

                    if (draft.selectedImageUris.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(draft.selectedImageUris) { uriStr ->
                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    AsyncImage(
                                        model = Uri.parse(uriStr),
                                        contentDescription = "Selected scene photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeImage(uriStr) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(26.dp)
                                            .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove image",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Duration Selector (Up to 30 Minutes / 1800s)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = DirectorAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strings.durationLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Surface(
                            color = DirectorAmber.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = strings.durationMinutesSeconds(draft.targetDurationSeconds),
                                style = MaterialTheme.typography.labelLarge,
                                color = DirectorAmber,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val durationPresets = listOf(15, 60, 180, 300, 600, 900, 1800)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        durationPresets.forEach { sec ->
                            FilterChip(
                                selected = draft.targetDurationSeconds == sec,
                                onClick = { viewModel.setDurationSeconds(sec) },
                                label = { Text(strings.durationMinutesSeconds(sec)) },
                                modifier = Modifier.testTag("duration_chip_$sec")
                            )
                        }
                    }

                    Slider(
                        value = draft.targetDurationSeconds.toFloat(),
                        onValueChange = { viewModel.setDurationSeconds(it.toInt()) },
                        valueRange = 10f..1800f,
                        modifier = Modifier.testTag("duration_slider")
                    )
                }
            }

            // 4. Video Quality, Format, Language & Audio Toggles
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${strings.qualityLabel} & ${strings.formatLabel}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VideoResolution.entries.forEach { res ->
                            FilterChip(
                                selected = draft.resolution == res,
                                onClick = { viewModel.setResolution(res) },
                                label = { Text(res.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("resolution_${res.name}")
                            )
                        }
                        VideoAspectRatio.entries.forEach { ratio ->
                            FilterChip(
                                selected = draft.aspectRatio == ratio,
                                onClick = { viewModel.setAspectRatio(ratio) },
                                label = {
                                    Text(
                                        if (appLanguage == AppLanguage.HINDI) ratio.labelHi else ratio.labelEn
                                    )
                                },
                                modifier = Modifier.testTag("aspect_${ratio.name}")
                            )
                        }
                    }

                    Text(
                        text = strings.storyLanguageLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StoryLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = draft.storyLanguage == lang,
                                onClick = { viewModel.setStoryLanguage(lang) },
                                label = {
                                    Text(if (appLanguage == AppLanguage.HINDI) lang.labelHi else lang.labelEn)
                                },
                                modifier = Modifier.testTag("story_lang_${lang.name}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = ElectricCyan)
                            Spacer(Modifier.width(10.dp))
                            Text(strings.hindiNarrationToggle, style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = draft.enableNarration,
                            onCheckedChange = viewModel::setEnableNarration,
                            modifier = Modifier.testTag("toggle_narration")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Subtitles, contentDescription = null, tint = DirectorAmber)
                            Spacer(Modifier.width(10.dp))
                            Text(strings.subtitlesToggle, style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(
                            checked = draft.enableSubtitles,
                            onCheckedChange = viewModel::setEnableSubtitles,
                            modifier = Modifier.testTag("toggle_subtitles")
                        )
                    }
                }
            }

            if (draft.errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = draft.errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Large Generate Video Button
            Button(
                onClick = {
                    viewModel.createAndOpenProject(
                        onCreated = onProjectCreated,
                        errorEmptyPrompt = strings.errorEmptyPrompt
                    )
                },
                enabled = !draft.isCreating,
                contentPadding = PaddingValues(vertical = 16.dp, horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = Color(0xFF001F26)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("generate_video_cta")
            ) {
                if (draft.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = Color(0xFF001F26)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(strings.analyzingStory, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = strings.generateVideoCta,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val borderColor = if (selected) ElectricCyan else MaterialTheme.colorScheme.outline
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}
