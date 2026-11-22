package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.CharacterProfileEntity
import com.example.data.model.SceneStatus
import com.example.data.model.VideoAspectRatio
import com.example.data.model.VideoResolution
import com.example.data.model.VideoSceneEntity
import com.example.localization.AppLanguage
import com.example.localization.StudioStrings
import com.example.ui.theme.DirectorAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioCoralError
import com.example.ui.theme.StudioEmerald
import com.example.ui.viewmodel.ProjectWorkspaceViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProjectWorkspaceScreen(
    viewModel: ProjectWorkspaceViewModel,
    strings: StudioStrings,
    appLanguage: AppLanguage,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val details by viewModel.projectDetails.collectAsStateWithLifecycle()
    val opState by viewModel.opState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCharacterDialog by remember { mutableStateOf(false) }
    var editingCharacter by remember { mutableStateOf<CharacterProfileEntity?>(null) }
    var editingScene by remember { mutableStateOf<VideoSceneEntity?>(null) }
    var previewClipPath by remember { mutableStateOf<String?>(null) }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
            } ?: "Audio Track"
            viewModel.setBackgroundMusic(uri, displayName)
        }
    }

    LaunchedEffect(opState.snackbarMessage) {
        opState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = details?.project?.title ?: "Studio Workspace",
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        details?.project?.let { p ->
                            Text(
                                text = "${p.resolution.label} • ${if (appLanguage == AppLanguage.HINDI) p.aspectRatio.labelHi else p.aspectRatio.labelEn}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("workspace_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val projectData = details
        if (projectData == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val project = projectData.project
        val scenes = projectData.scenes.sortedBy { it.sceneIndex }
        val characters = projectData.characters.sortedBy { it.orderIndex }
        val completedClipsCount = scenes.count {
            it.status == SceneStatus.COMPLETED && !it.clipFilePath.isNullOrBlank() && File(it.clipFilePath).exists()
        }
        val finalFile = project.finalVideoPath?.let(::File)?.takeIf { it.exists() && it.length() > 0L }
        val activePlayerPath = previewClipPath?.takeIf { File(it).exists() } ?: finalFile?.absolutePath

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Offline AI Structurer Notice (required transparency when Firebase AI Logic is declined)
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        border = BorderStroke(1.dp, DirectorAmber.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = DirectorAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = strings.aiPlaceholderNoticeTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DirectorAmber,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.aiPlaceholderNoticeBody,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // 2. Final Video Preview Player & Combine / Real Download MP4 Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.previewTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (finalFile != null) {
                                    Text(
                                        text = "${(finalFile.length() / 1024).coerceAtLeast(1)} KB MP4",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = StudioEmerald
                                    )
                                }
                            }

                            if (activePlayerPath != null) {
                                VideoPlayerSection(
                                    videoPath = activePlayerPath,
                                    aspectRatio = project.aspectRatio
                                )
                                if (previewClipPath != null && previewClipPath != finalFile?.absolutePath) {
                                    TextButton(onClick = { previewClipPath = null }) {
                                        Text("Switch to Final Combined Video")
                                    }
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MovieCreation,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Text(
                                            text = strings.noVideoPreviewHint,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Combine Scenes + Real Download MP4 + Share Row
                            Button(
                                onClick = { viewModel.combineScenesOnly() },
                                enabled = completedClipsCount > 0 && !opState.isCombining && !opState.isBatchRendering,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("combine_scenes_button")
                            ) {
                                if (opState.isCombining) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.combiningVideos)
                                } else {
                                    Icon(Icons.Default.Merge, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${strings.combineScenesButton} ($completedClipsCount/${scenes.size})")
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.downloadMp4ToGallery(strings.savedToGalleryMessage)
                                    },
                                    enabled = finalFile != null && !opState.isExporting,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StudioEmerald,
                                        contentColor = Color(0xFF042411)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("download_mp4_button")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.downloadMp4Button, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        finalFile?.let { file ->
                                            viewModel.getShareUri(file.absolutePath)?.let { shareUri ->
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "video/mp4"
                                                    putExtra(Intent.EXTRA_STREAM, shareUri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(intent, strings.shareMp4Button))
                                            }
                                        }
                                    },
                                    enabled = finalFile != null,
                                    modifier = Modifier.testTag("share_mp4_button")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = strings.shareMp4Button, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.shareMp4Button)
                                }
                            }
                        }
                    }
                }

                // 3. Audio, Background Music & Export Quality Bar
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = ElectricCyan)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(strings.backgroundMusicLabel, style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            text = project.backgroundMusicName ?: "None",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (project.backgroundMusicUri != null) {
                                        TextButton(onClick = { viewModel.setBackgroundMusic(null, null) }) {
                                            Text(strings.clearMusicButton)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { audioPicker.launch(arrayOf("audio/*")) },
                                        modifier = Modifier.testTag("pick_music_button")
                                    ) {
                                        Text(strings.pickMusicButton)
                                    }
                                }
                            }

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                VideoResolution.entries.forEach { res ->
                                    FilterChip(
                                        selected = project.resolution == res,
                                        onClick = { viewModel.updateResolutionAndAspect(res, project.aspectRatio) },
                                        label = { Text(res.label) }
                                    )
                                }
                                VideoAspectRatio.entries.forEach { ratio ->
                                    FilterChip(
                                        selected = project.aspectRatio == ratio,
                                        onClick = { viewModel.updateResolutionAndAspect(project.resolution, ratio) },
                                        label = {
                                            Text(if (appLanguage == AppLanguage.HINDI) ratio.labelHi else ratio.labelEn)
                                        }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.hindiNarrationToggle, style = MaterialTheme.typography.bodySmall)
                                Switch(
                                    checked = project.enableNarration,
                                    onCheckedChange = { viewModel.updateAudioSubtitleToggles(it, project.enableSubtitles) }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(strings.subtitlesToggle, style = MaterialTheme.typography.bodySmall)
                                Switch(
                                    checked = project.enableSubtitles,
                                    onCheckedChange = { viewModel.updateAudioSubtitleToggles(project.enableNarration, it) }
                                )
                            }
                        }
                    }
                }

                // 4. Consistent Characters Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.charactersSectionTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = {
                                editingCharacter = null
                                showCharacterDialog = true
                            },
                            modifier = Modifier.testTag("add_character_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(strings.addCharacter)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(characters, key = { it.id }) { character ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.width(230.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (character.referenceImageUri != null) {
                                            AsyncImage(
                                                model = Uri.parse(character.referenceImageUri),
                                                contentDescription = character.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )
                                        } else {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = ElectricCyan)
                                        }
                                        Text(
                                            text = character.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1
                                        )
                                        IconButton(
                                            onClick = {
                                                editingCharacter = character
                                                showCharacterDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit character", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteCharacter(character.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete character", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    Text(
                                        text = character.appearancePrompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Batch Render Control & Progress
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.scenesSectionTitle} (${scenes.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(
                                onClick = { viewModel.addNewScene() },
                                modifier = Modifier.testTag("add_scene_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.addScene)
                            }
                        }

                        Button(
                            onClick = { viewModel.renderAllScenesAndCombine(autoCombine = true) },
                            enabled = scenes.isNotEmpty() && !opState.isBatchRendering && !opState.isCombining,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF001F26)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("render_all_scenes_button")
                        ) {
                            Icon(Icons.Default.MovieCreation, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.generateAllScenes, fontWeight = FontWeight.Bold)
                        }

                        if (opState.isBatchRendering || opState.isCombining || opState.activeSceneId != null) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = opState.statusText.ifBlank { "Rendering..." },
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (opState.isBatchRendering) {
                                            IconButton(onClick = { viewModel.cancelBatchRender() }) {
                                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                                            }
                                        }
                                    }
                                    LinearProgressIndicator(
                                        progress = { opState.progress.coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        opState.errorMessage?.let { err ->
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StudioCoralError)
                                    Text(
                                        text = err,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Scene Cards List
                items(scenes, key = { it.id }) { scene ->
                    val isThisRendering = opState.activeSceneId == scene.id
                    val hasClip = scene.status == SceneStatus.COMPLETED &&
                        !scene.clipFilePath.isNullOrBlank() &&
                        File(scene.clipFilePath).exists()

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scene_card_${scene.sceneIndex}")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${scene.sceneIndex + 1} • ${scene.title}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    color = when (scene.status) {
                                        SceneStatus.COMPLETED -> StudioEmerald.copy(alpha = 0.16f)
                                        SceneStatus.FAILED -> StudioCoralError.copy(alpha = 0.16f)
                                        else -> ElectricCyan.copy(alpha = 0.14f)
                                    },
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "${scene.durationSeconds}s • ${scene.status.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (scene.status) {
                                            SceneStatus.COMPLETED -> StudioEmerald
                                            SceneStatus.FAILED -> StudioCoralError
                                            else -> ElectricCyan
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (scene.sourceImageUri != null) {
                                    AsyncImage(
                                        model = Uri.parse(scene.sourceImageUri),
                                        contentDescription = "Scene reference image",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = scene.visualPrompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3
                                    )
                                    if (scene.narrationText.isNotBlank()) {
                                        Text(
                                            text = "Voice: ${scene.narrationText}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DirectorAmber,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }

                            if (scene.errorMessage != null) {
                                Text(
                                    text = scene.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StudioCoralError
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { editingScene = scene }) {
                                        Icon(Icons.Default.Edit, contentDescription = strings.editScene, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { viewModel.deleteScene(scene.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete scene", modifier = Modifier.size(18.dp))
                                    }
                                    if (hasClip) {
                                        TextButton(onClick = { previewClipPath = scene.clipFilePath }) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(2.dp))
                                            Text("Preview")
                                        }
                                    }
                                }

                                OutlinedButton(
                                    onClick = { viewModel.renderSingleScene(scene.id) },
                                    enabled = !isThisRendering && !opState.isBatchRendering,
                                    modifier = Modifier.testTag("render_scene_${scene.sceneIndex}")
                                ) {
                                    if (isThisRendering) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else if (scene.status == SceneStatus.FAILED) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(strings.retryScene)
                                    } else {
                                        if (hasClip) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                        }
                                        Text(strings.renderSceneClip)
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(28.dp)) }
            }
        }
    }

    if (showCharacterDialog) {
        EditCharacterDialog(
            initial = editingCharacter,
            strings = strings,
            onDismiss = { showCharacterDialog = false },
            onSave = { name, appearance, voice, image ->
                viewModel.addOrUpdateCharacter(editingCharacter?.id, name, appearance, voice, image)
                showCharacterDialog = false
            }
        )
    }

    editingScene?.let { sceneToEdit ->
        EditSceneDialog(
            scene = sceneToEdit,
            strings = strings,
            appLanguage = appLanguage,
            onDismiss = { editingScene = null },
            onSave = { updated ->
                viewModel.updateScene(updated)
                editingScene = null
            }
        )
    }
}
