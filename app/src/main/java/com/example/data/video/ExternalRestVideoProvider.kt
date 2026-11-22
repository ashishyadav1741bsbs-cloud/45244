package com.example.data.video

import com.example.data.model.ProviderIds
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@JsonClass(generateAdapter = true)
data class ExternalGenerateRequest(
    val model: String,
    val prompt: String,
    val durationSeconds: Int,
    val width: Int,
    val height: Int,
    val language: String,
    val narrationText: String
)

@JsonClass(generateAdapter = true)
data class ExternalJobResponse(
    val jobId: String? = null,
    val status: String? = null,
    val videoUrl: String? = null,
    val error: String? = null
)

/**
 * Modular REST API connector for third-party AI Video Generation backends (e.g., Flow/Veo/Runway/Kling proxy).
 * Submits real HTTP POST /generate requests, polls GET /jobs/{jobId} with retry, and downloads MP4 bytes.
 * Never fakes responses when unconfigured.
 */
class ExternalRestVideoProvider : VideoGenerationProvider {

    override val id: String = ProviderIds.EXTERNAL_REST
    override val displayNameEn: String = "External AI Video API (REST Endpoint)"
    override val displayNameHi: String = "बाहरी AI वीडियो API (REST एंडपॉइंट)"
    override val limitsNoteEn: String = "Uses your configured HTTPS endpoint & API key. External provider quotas and pricing apply."
    override val limitsNoteHi: String = "आपके द्वारा सेट किए गए HTTPS एंडपॉइंट और API Key का उपयोग करता है। बाहरी प्रोवाइडर की सीमाएँ लागू होंगी।"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val reqAdapter = moshi.adapter(ExternalGenerateRequest::class.java)
    private val resAdapter = moshi.adapter(ExternalJobResponse::class.java)

    override suspend fun generateSceneClip(
        request: SceneGenerationRequest,
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = request.providerConfig.externalEndpointUrl.trim().trimEnd('/')
            require(baseUrl.startsWith("https://") || baseUrl.startsWith("http://")) {
                "External AI Video API endpoint is not configured. Open the Providers tab to enter your HTTPS endpoint URL, or switch to the On-Device MP4 Studio Renderer."
            }

            val (w, h) = request.project.resolution.dimensionsFor(request.project.aspectRatio)
            val charContext = request.characters.joinToString("; ") { "${it.name}: ${it.appearancePrompt}" }
            val fullPrompt = buildString {
                append(request.scene.visualPrompt)
                if (charContext.isNotBlank()) append(" | Characters: $charContext")
            }

            val payload = ExternalGenerateRequest(
                model = request.providerConfig.externalModelName.ifBlank { "veo-3" },
                prompt = fullPrompt,
                durationSeconds = request.scene.durationSeconds,
                width = w,
                height = h,
                language = request.project.language.name,
                narrationText = request.scene.narrationText
            )

            onProgress(0.15f, "Submitting clip job to external provider...")
            val submitReqBuilder = Request.Builder()
                .url("$baseUrl/generate")
                .post(reqAdapter.toJson(payload).toRequestBody("application/json".toMediaType()))
            if (request.providerConfig.externalApiKey.isNotBlank()) {
                submitReqBuilder.header("Authorization", "Bearer ${request.providerConfig.externalApiKey}")
            }

            val submitBody = client.newCall(submitReqBuilder.build()).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    error("Provider HTTP ${resp.code}: ${bodyStr.take(180)}")
                }
                bodyStr
            }

            var jobState = resAdapter.fromJson(submitBody)
                ?: error("Invalid JSON response from external video provider.")
            if (!jobState.error.isNullOrBlank()) error(jobState.error!!)

            // Poll job status if videoUrl isn't returned immediately
            var attempts = 0
            while (jobState.videoUrl.isNullOrBlank() && attempts < 60) {
                val jobId = jobState.jobId ?: error("Provider did not return a jobId or videoUrl.")
                attempts++
                onProgress(0.25f + (attempts / 60f) * 0.55f, "Waiting for cloud render ($attempts)...")
                delay(3_000L)

                val pollReq = Request.Builder().url("$baseUrl/jobs/$jobId").get().apply {
                    if (request.providerConfig.externalApiKey.isNotBlank()) {
                        header("Authorization", "Bearer ${request.providerConfig.externalApiKey}")
                    }
                }.build()

                val pollBody = client.newCall(pollReq).execute().use { resp ->
                    val str = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) error("Job poll HTTP ${resp.code}: ${str.take(160)}")
                    str
                }
                jobState = resAdapter.fromJson(pollBody) ?: error("Invalid job status JSON.")
                if (jobState.status.equals("failed", ignoreCase = true)) {
                    error(jobState.error ?: "External video job failed.")
                }
            }

            val downloadUrl = jobState.videoUrl ?: error("External provider timed out before returning videoUrl.")
            onProgress(0.85f, "Downloading rendered MP4...")

            val dlReq = Request.Builder().url(downloadUrl).get().build()
            val outFile = request.outputFile
            outFile.parentFile?.mkdirs()
            client.newCall(dlReq).execute().use { resp ->
                if (!resp.isSuccessful) error("Download HTTP ${resp.code}")
                val stream = resp.body?.byteStream() ?: error("Empty video download body.")
                outFile.outputStream().use { out -> stream.copyTo(out) }
            }

            check(outFile.exists() && outFile.length() > 0L) { "Downloaded MP4 file is empty." }
            onProgress(1f, "Cloud clip downloaded")
            outFile
        }
    }

    suspend fun testEndpointReachable(endpointUrl: String, apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = endpointUrl.trim().trimEnd('/')
            require(clean.startsWith("https://") || clean.startsWith("http://")) {
                "Enter a valid http:// or https:// URL"
            }
            val req = Request.Builder().url("$clean/health").get().apply {
                if (apiKey.isNotBlank()) header("Authorization", "Bearer $apiKey")
            }.build()
            client.newCall(req).execute().use { resp ->
                "HTTP ${resp.code} ${resp.message}"
            }
        }
    }
}
