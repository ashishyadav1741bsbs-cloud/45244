package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ProviderConfig
import com.example.data.model.ProviderIds
import com.example.localization.AppLanguage
import com.example.localization.StudioStrings
import com.example.ui.theme.DirectorAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.viewmodel.ProvidersViewModel

@Composable
fun ProvidersScreen(
    viewModel: ProvidersViewModel,
    strings: StudioStrings,
    appLanguage: AppLanguage,
    modifier: Modifier = Modifier
) {
    val config by viewModel.providerConfig.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()

    var selectedProviderId by remember(config.activeProviderId) { mutableStateOf(config.activeProviderId) }
    var endpointUrl by remember(config.externalEndpointUrl) { mutableStateOf(config.externalEndpointUrl) }
    var apiKey by remember(config.externalApiKey) { mutableStateOf(config.externalApiKey) }
    var modelName by remember(config.externalModelName) { mutableStateOf(config.externalModelName) }

    LaunchedEffect(config) {
        selectedProviderId = config.activeProviderId
        endpointUrl = config.externalEndpointUrl
        apiKey = config.externalApiKey
        modelName = config.externalModelName
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = strings.providersTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = strings.providersSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Zero-Credit Policy Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioEmerald.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, StudioEmerald.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StudioEmerald)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = strings.zeroCreditPolicyTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = StudioEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.zeroCreditPolicyBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Provider Option 1: On-Device H.264 MP4 Renderer
            ProviderChoiceCard(
                selected = selectedProviderId == ProviderIds.ON_DEVICE,
                title = if (appLanguage == AppLanguage.HINDI) {
                    "ऑन-डिवाइस MP4 स्टूडियो रेंडरर (H.264)"
                } else {
                    "On-Device MP4 Studio Renderer (H.264)"
                },
                description = if (appLanguage == AppLanguage.HINDI) {
                    "फ़ोन पर ही असली MP4 क्लिप, फ़ोटो मोशन, हिंदी वॉइसओवर और सबटाइटल बनाता है। 100% मुफ़्त और अनलिमिटेड।"
                } else {
                    "Generates real H.264 MP4 clips, Ken Burns photo motion, TTS voiceover & subtitles directly on your phone. Free & unlimited."
                },
                icon = Icons.Default.Memory,
                testTag = "provider_on_device",
                onClick = {
                    selectedProviderId = ProviderIds.ON_DEVICE
                    viewModel.saveConfig(
                        ProviderConfig(
                            activeProviderId = ProviderIds.ON_DEVICE,
                            externalEndpointUrl = endpointUrl,
                            externalApiKey = apiKey,
                            externalModelName = modelName
                        )
                    )
                }
            )

            // Provider Option 2: External REST Video API
            ProviderChoiceCard(
                selected = selectedProviderId == ProviderIds.EXTERNAL_REST,
                title = if (appLanguage == AppLanguage.HINDI) {
                    "बाहरी AI वीडियो API (REST एंडपॉइंट)"
                } else {
                    "External AI Video API (REST Endpoint)"
                },
                description = if (appLanguage == AppLanguage.HINDI) {
                    "किसी भी कस्टम AI वीडियो जनरेशन बैकएंड (POST /generate व GET /jobs/{id}) से जुड़ें। बाहरी प्रोवाइडर की लागत/सीमाएँ लागू होंगी।"
                } else {
                    "Connect any Flow/Veo/Runway-compatible HTTPS REST backend (POST /generate & GET /jobs/{id}). External provider quotas apply."
                },
                icon = Icons.Default.CloudQueue,
                testTag = "provider_external_rest",
                onClick = {
                    selectedProviderId = ProviderIds.EXTERNAL_REST
                    viewModel.saveConfig(
                        ProviderConfig(
                            activeProviderId = ProviderIds.EXTERNAL_REST,
                            externalEndpointUrl = endpointUrl,
                            externalApiKey = apiKey,
                            externalModelName = modelName
                        )
                    )
                }
            )

            // External REST Endpoint Config Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.externalConfigTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = endpointUrl,
                        onValueChange = { endpointUrl = it },
                        label = { Text(strings.endpointUrlLabel) },
                        placeholder = { Text("https://your-video-backend.example.com/api") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("external_endpoint_input")
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text(strings.apiKeyLabel) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("external_api_key_input")
                    )

                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text(strings.modelNameLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("external_model_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveConfig(
                                    ProviderConfig(
                                        activeProviderId = selectedProviderId,
                                        externalEndpointUrl = endpointUrl,
                                        externalApiKey = apiKey,
                                        externalModelName = modelName
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_provider_button")
                        ) {
                            Text(strings.saveProviderSettings)
                        }

                        OutlinedButton(
                            onClick = { viewModel.testConnection(endpointUrl, apiKey) },
                            enabled = !isTesting,
                            modifier = Modifier.testTag("test_provider_button")
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(strings.testConnection)
                        }
                    }

                    statusMessage?.let { msg ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProviderChoiceCard(
    selected: Boolean,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) ElectricCyan else MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) ElectricCyan else DirectorAmber,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}
