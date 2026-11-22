package com.example.data.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.example.data.model.ProviderIds
import com.example.data.model.StoryLanguage
import com.example.data.model.TransitionStyle
import com.example.data.model.VideoAspectRatio
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Real on-device H.264/AVC MP4 video renderer using Android MediaCodec Surface input,
 * Ken Burns photo animation, Hindi/English burned-in subtitles, transition effects,
 * and optional Android TextToSpeech voiceover muxed into AAC audio.
 */
class OnDeviceMp4SceneProvider(
    private val context: Context
) : VideoGenerationProvider {

    override val id: String = ProviderIds.ON_DEVICE
    override val displayNameEn: String = "On-Device MP4 Studio Renderer (H.264)"
    override val displayNameHi: String = "ऑन-डिवाइस MP4 स्टूडियो रेंडरर (H.264)"
    override val limitsNoteEn: String = "Free, unlimited offline rendering on your phone hardware (720p/1080p)."
    override val limitsNoteHi: String = "आपके फ़ोन पर पूरी तरह मुफ़्त और अनलिमिटेड ऑफ़लाइन रेंडरिंग (720p/1080p)।"

    override suspend fun generateSceneClip(
        request: SceneGenerationRequest,
        onProgress: (Float, String) -> Unit
    ): Result<File> = withContext(Dispatchers.Default) {
        runCatching {
            val outputFile = request.outputFile
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) outputFile.delete()

            val (requestedWidth, requestedHeight) = request.project.resolution.dimensionsFor(request.project.aspectRatio)
            val durationSec = request.scene.durationSeconds.coerceIn(2, 1800)
            val fps = if (durationSec > 20) 12 else 20
            val totalFrames = max(fps * 2, durationSec * fps)

            onProgress(0.05f, "Preparing canvas & visual assets...")
            val sourceBitmap = request.scene.sourceImageUri?.let { uriStr ->
                loadScaledBitmap(uriStr, requestedWidth, requestedHeight)
            }

            // Optional TTS narration synthesis to WAV, then encoded to AAC alongside video
            var wavFile: File? = null
            if (request.project.enableNarration && request.scene.narrationText.isNotBlank()) {
                onProgress(0.12f, "Synthesizing voice narration...")
                wavFile = synthesizeTtsWav(
                    text = request.scene.narrationText,
                    language = request.project.language,
                    targetFile = File(context.cacheDir, "tts_${UUID.randomUUID()}.wav")
                )
            }

            val videoOnlyFile = if (wavFile != null) {
                File(context.cacheDir, "vid_${UUID.randomUUID()}.mp4")
            } else {
                outputFile
            }

            try {
                encodeH264Video(
                    outputFile = videoOnlyFile,
                    width = requestedWidth,
                    height = requestedHeight,
                    bitrate = request.project.resolution.bitrateBps,
                    fps = fps,
                    totalFrames = totalFrames,
                    request = request,
                    sourceBitmap = sourceBitmap,
                    onFrameProgress = { frameFraction ->
                        val scaled = 0.2f + (frameFraction * (if (wavFile != null) 0.65f else 0.78f))
                        onProgress(scaled, "Encoding H.264 video (${(frameFraction * 100).toInt()}%)...")
                    }
                )
            } finally {
                sourceBitmap?.recycle()
            }

            if (wavFile != null && wavFile.exists() && wavFile.length() > 44L) {
                onProgress(0.88f, "Muxing voiceover audio track...")
                val aacFile = File(context.cacheDir, "aac_${UUID.randomUUID()}.m4a")
                try {
                    val encodedAac = encodeWavToAacMp4(wavFile, aacFile)
                    if (encodedAac) {
                        muxVideoAndAudio(videoOnlyFile, aacFile, outputFile)
                    } else {
                        videoOnlyFile.copyTo(outputFile, overwrite = true)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Audio mux fallback to video-only: ${e.message}")
                    videoOnlyFile.copyTo(outputFile, overwrite = true)
                } finally {
                    wavFile.delete()
                    aacFile.delete()
                    if (videoOnlyFile != outputFile) videoOnlyFile.delete()
                }
            }

            check(outputFile.exists() && outputFile.length() > 0L) {
                "Rendered MP4 file is empty."
            }
            onProgress(1f, "Scene clip ready")
            outputFile
        }
    }

    private suspend fun encodeH264Video(
        outputFile: File,
        width: Int,
        height: Int,
        bitrate: Int,
        fps: Int,
        totalFrames: Int,
        request: SceneGenerationRequest,
        sourceBitmap: Bitmap?,
        onFrameProgress: (Float) -> Unit
    ) {
        val (safeWidth, safeHeight) = createConfiguredEncoder(width, height, bitrate, fps)
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, safeWidth, safeHeight).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        val inputSurface = encoder.createInputSurface()
        encoder.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false
        val bufferInfo = MediaCodec.BufferInfo()

        val palette = sceneColorPalette(request.scene.sceneIndex)
        val painter = SceneCanvasPainter(
            width = safeWidth,
            height = safeHeight,
            palette = palette,
            request = request,
            sourceBitmap = sourceBitmap
        )

        try {
            val frameDurationUs = 1_000_000L / fps
            for (frameIdx in 0 until totalFrames) {
                currentCoroutineContext().ensureActive()

                val canvas = inputSurface.lockCanvas(null)
                try {
                    val progress = frameIdx.toFloat() / max(1, totalFrames - 1).toFloat()
                    painter.drawFrame(canvas, progress, frameIdx)
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                // Drain available encoded buffers without blocking unnecessarily
                drainEncoder(
                    encoder = encoder,
                    muxer = muxer,
                    bufferInfo = bufferInfo,
                    endOfStream = false,
                    trackIndexRef = { trackIndex },
                    setTrackIndex = { trackIndex = it },
                    muxerStartedRef = { muxerStarted },
                    setMuxerStarted = { muxerStarted = it },
                    basePtsUs = frameIdx * frameDurationUs
                )

                if (frameIdx % max(1, totalFrames / 20) == 0 || frameIdx == totalFrames - 1) {
                    onFrameProgress((frameIdx + 1).toFloat() / totalFrames.toFloat())
                }
            }

            encoder.signalEndOfInputStream()
            drainEncoder(
                encoder = encoder,
                muxer = muxer,
                bufferInfo = bufferInfo,
                endOfStream = true,
                trackIndexRef = { trackIndex },
                setTrackIndex = { trackIndex = it },
                muxerStartedRef = { muxerStarted },
                setMuxerStarted = { muxerStarted = it },
                basePtsUs = totalFrames * (1_000_000L / fps)
            )
        } finally {
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            runCatching { inputSurface.release() }
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
        }
    }

    private fun createConfiguredEncoder(reqW: Int, reqH: Int, bitrate: Int, fps: Int): Pair<Int, Int> {
        val candidates = listOf(
            reqW to reqH,
            if (reqW > reqH) 1280 to 720 else 720 to 1280,
            if (reqW > reqH) 854 to 480 else 480 to 854
        )
        for ((w, h) in candidates) {
            var testCodec: MediaCodec? = null
            try {
                testCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
                val fmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                    setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                    setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                }
                testCodec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                return w to h
            } catch (_: Exception) {
                // Fallback to next supported resolution
            } finally {
                runCatching { testCodec?.release() }
            }
        }
        return if (reqW > reqH) 1280 to 720 else 720 to 1280
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        endOfStream: Boolean,
        trackIndexRef: () -> Int,
        setTrackIndex: (Int) -> Unit,
        muxerStartedRef: () -> Boolean,
        setMuxerStarted: (Boolean) -> Unit,
        basePtsUs: Long
    ) {
        val timeoutUs = if (endOfStream) 10_000L else 0L
        while (true) {
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!muxerStartedRef()) {
                        val newFormat = encoder.outputFormat
                        val idx = muxer.addTrack(newFormat)
                        setTrackIndex(idx)
                        muxer.start()
                        setMuxerStarted(true)
                    }
                }
                outputIndex >= 0 -> {
                    val encodedData = encoder.getOutputBuffer(outputIndex)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        bufferInfo.size = 0
                    }
                    if (bufferInfo.size != 0 && muxerStartedRef() && encodedData != null) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        if (bufferInfo.presentationTimeUs <= 0L) {
                            bufferInfo.presentationTimeUs = basePtsUs
                        }
                        muxer.writeSampleData(trackIndexRef(), encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(outputIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        }
    }

    private fun loadScaledBitmap(uriStr: String, targetW: Int, targetH: Int): Bitmap? {
        return runCatching {
            val uri = Uri.parse(uriStr)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            var sampleSize = 1
            while (bounds.outWidth / sampleSize > targetW * 2 || bounds.outHeight / sampleSize > targetH * 2) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
        }.getOrNull()
    }

    private suspend fun synthesizeTtsWav(
        text: String,
        language: StoryLanguage,
        targetFile: File
    ): File? = withTimeoutOrNull(7_000L) {
        suspendCancellableCoroutine { cont ->
            var tts: TextToSpeech? = null
            tts = TextToSpeech(context) { status ->
                if (status != TextToSpeech.SUCCESS) {
                    runCatching { tts?.shutdown() }
                    if (cont.isActive) cont.resume(null)
                    return@TextToSpeech
                }
                val locale = when (language) {
                    StoryLanguage.HINDI, StoryLanguage.HINGLISH -> Locale("hi", "IN")
                    StoryLanguage.ENGLISH -> Locale.US
                }
                val langSupport = tts?.setLanguage(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
                if (langSupport == TextToSpeech.LANG_MISSING_DATA || langSupport == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }

                val utteranceId = UUID.randomUUID().toString()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) {
                        runCatching { tts?.shutdown() }
                        if (cont.isActive) cont.resume(targetFile)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(id: String?) {
                        runCatching { tts?.shutdown() }
                        if (cont.isActive) cont.resume(null)
                    }
                    override fun onError(id: String?, errorCode: Int) {
                        runCatching { tts?.shutdown() }
                        if (cont.isActive) cont.resume(null)
                    }
                })
                val res = tts?.synthesizeToFile(text.take(450), Bundle(), targetFile, utteranceId)
                if (res != TextToSpeech.SUCCESS) {
                    runCatching { tts?.shutdown() }
                    if (cont.isActive) cont.resume(null)
                }
            }
            cont.invokeOnCancellation {
                runCatching { tts?.shutdown() }
            }
        }
    }

    private fun encodeWavToAacMp4(wavFile: File, outM4a: File): Boolean {
        val fis = FileInputStream(wavFile)
        val header = ByteArray(44)
        if (fis.read(header) < 44) {
            fis.close()
            return false
        }
        val channels = ((header[23].toInt() and 0xFF) shl 8) or (header[22].toInt() and 0xFF)
        val sampleRate = ((header[27].toInt() and 0xFF) shl 24) or
            ((header[26].toInt() and 0xFF) shl 16) or
            ((header[25].toInt() and 0xFF) shl 8) or
            (header[24].toInt() and 0xFF)

        val safeRate = if (sampleRate in 8000..48000) sampleRate else 22050
        val safeChannels = if (channels in 1..2) channels else 1
        val pcmBytes = fis.readBytes()
        fis.close()
        if (pcmBytes.isEmpty()) return false

        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, safeRate, safeChannels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 96_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        val muxer = MediaMuxer(outM4a.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var audioTrack = -1
        var muxerStarted = false
        val info = MediaCodec.BufferInfo()
        var offset = 0
        var eosSent = false
        val bytesPerSample = 2 * safeChannels
        var totalSamples = 0L

        try {
            while (true) {
                if (!eosSent) {
                    val inIdx = codec.dequeueInputBuffer(5_000)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx)
                        if (offset >= pcmBytes.size) {
                            codec.queueInputBuffer(inIdx, 0, 0, (totalSamples * 1_000_000L) / safeRate, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            eosSent = true
                        } else if (buf != null) {
                            buf.clear()
                            val chunk = min(buf.remaining(), pcmBytes.size - offset)
                            buf.put(pcmBytes, offset, chunk)
                            val ptsUs = (totalSamples * 1_000_000L) / safeRate
                            codec.queueInputBuffer(inIdx, 0, chunk, ptsUs, 0)
                            offset += chunk
                            totalSamples += chunk / max(1, bytesPerSample)
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 5_000)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            audioTrack = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    }
                    outIdx >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIdx)
                        if ((info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) info.size = 0
                        if (info.size > 0 && muxerStarted && outBuf != null) {
                            outBuf.position(info.offset)
                            outBuf.limit(info.offset + info.size)
                            muxer.writeSampleData(audioTrack, outBuf, info)
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
                    }
                }
            }
        } finally {
            runCatching { codec.stop() }
            runCatching { codec.release() }
            if (muxerStarted) runCatching { muxer.stop() }
            runCatching { muxer.release() }
        }
        return outM4a.exists() && outM4a.length() > 0L
    }

    private fun muxVideoAndAudio(videoFile: File, audioFile: File, outputFile: File) {
        val vExtractor = MediaExtractor()
        val aExtractor = MediaExtractor()
        vExtractor.setDataSource(videoFile.absolutePath)
        aExtractor.setDataSource(audioFile.absolutePath)

        val vTrackSrc = (0 until vExtractor.trackCount).firstOrNull {
            vExtractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
        } ?: error("Missing video track")
        val aTrackSrc = (0 until aExtractor.trackCount).firstOrNull {
            aExtractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: error("Missing audio track")

        vExtractor.selectTrack(vTrackSrc)
        aExtractor.selectTrack(aTrackSrc)

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val vTrackDst = muxer.addTrack(vExtractor.getTrackFormat(vTrackSrc))
        val aTrackDst = muxer.addTrack(aExtractor.getTrackFormat(aTrackSrc))
        muxer.start()

        val buffer = ByteBuffer.allocate(512 * 1024)
        val info = MediaCodec.BufferInfo()
        var maxVideoPtsUs = 0L

        while (true) {
            val size = vExtractor.readSampleData(buffer, 0)
            if (size < 0) break
            info.offset = 0
            info.size = size
            info.presentationTimeUs = vExtractor.sampleTime
            info.flags = if ((vExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                MediaCodec.BUFFER_FLAG_KEY_FRAME
            } else 0
            maxVideoPtsUs = max(maxVideoPtsUs, info.presentationTimeUs)
            muxer.writeSampleData(vTrackDst, buffer, info)
            vExtractor.advance()
        }

        while (true) {
            val size = aExtractor.readSampleData(buffer, 0)
            if (size < 0) break
            val pts = aExtractor.sampleTime
            if (pts > maxVideoPtsUs + 250_000L) break
            info.offset = 0
            info.size = size
            info.presentationTimeUs = pts
            info.flags = 0
            muxer.writeSampleData(aTrackDst, buffer, info)
            aExtractor.advance()
        }

        muxer.stop()
        muxer.release()
        vExtractor.release()
        aExtractor.release()
    }

    private class SceneCanvasPainter(
        private val width: Int,
        private val height: Int,
        private val palette: Pair<Int, Int>,
        private val request: SceneGenerationRequest,
        private val sourceBitmap: Bitmap?
    ) {
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(175, 11, 15, 26) }
        private val subtitleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(190, 8, 12, 22) }
        private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#00E5FF") }
        private val fadePaint = Paint()

        private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = max(24f, width * 0.028f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        private val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFB300")
            textSize = max(18f, width * 0.02f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        private val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = max(22f, min(width, height) * 0.038f)
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        private val matrix = Matrix()

        fun drawFrame(canvas: Canvas, progress: Float, frameIdx: Int) {
            val w = width.toFloat()
            val h = height.toFloat()

            // 1. Base Backdrop or Ken-Burns Photo Animation
            if (sourceBitmap != null) {
                canvas.drawColor(Color.BLACK)
                val bw = sourceBitmap.width.toFloat()
                val bh = sourceBitmap.height.toFloat()
                val baseScale = max(w / bw, h / bh)
                val zoomScale = baseScale * (1.0f + 0.12f * progress)
                val dx = (w - bw * zoomScale) * (0.5f + 0.08f * sin(progress * PI.toFloat()))
                val dy = (h - bh * zoomScale) * 0.5f
                matrix.reset()
                matrix.postScale(zoomScale, zoomScale)
                matrix.postTranslate(dx, dy)
                canvas.drawBitmap(sourceBitmap, matrix, bitmapPaint)
            } else {
                bgPaint.shader = LinearGradient(
                    0f, 0f, w, h,
                    palette.first, palette.second,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w, h, bgPaint)

                // Cinematic moving volumetric glow orbs
                val cx1 = w * (0.25f + 0.5f * progress)
                val cy1 = h * (0.35f + 0.1f * sin(progress * 2f * PI.toFloat()))
                glowPaint.shader = android.graphics.RadialGradient(
                    cx1, cy1, w * 0.42f,
                    Color.argb(115, 0, 229, 255), Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(cx1, cy1, w * 0.42f, glowPaint)

                val cx2 = w * (0.8f - 0.45f * progress)
                val cy2 = h * (0.65f + 0.08f * cos(progress * 2f * PI.toFloat()))
                glowPaint.shader = android.graphics.RadialGradient(
                    cx2, cy2, w * 0.36f,
                    Color.argb(100, 255, 179, 0), Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
                )
                canvas.drawCircle(cx2, cy2, w * 0.36f, glowPaint)

                // Subtle cinema film-strip bars
                val barPaint = Paint().apply { color = Color.argb(40, 255, 255, 255) }
                val waveY = h * 0.52f
                for (i in 0 until 24) {
                    val x = (i / 24f) * w + (progress * 40f)
                    val barH = 24f + 46f * kotlin.math.abs(sin((i * 0.5f) + progress * 6f))
                    canvas.drawRoundRect(x, waveY - barH / 2f, x + 8f, waveY + barH / 2f, 4f, 4f, barPaint)
                }
            }

            // 2. Top Studio Scene Header Pill
            val pad = w * 0.04f
            val pillRect = RectF(pad, pad, w - pad, pad + h * 0.12f)
            canvas.drawRoundRect(pillRect, 20f, 20f, badgeBgPaint)
            val sceneTag = "SCENE ${request.scene.sceneIndex + 1} • ${request.project.resolution.label}"
            canvas.drawText(sceneTag, pad + 24f, pad + metaPaint.textSize + 12f, metaPaint)
            canvas.drawText(
                request.scene.title.take(44),
                pad + 24f,
                pad + metaPaint.textSize + titlePaint.textSize + 24f,
                titlePaint
            )

            // Character consistency pill if characters exist
            val charNames = request.characters.joinToString(", ") { it.name }.take(40)
            if (charNames.isNotBlank() && sourceBitmap == null) {
                val charPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#D6E4FF")
                    textSize = max(18f, width * 0.02f)
                }
                canvas.drawText("Characters: $charNames", pad + 24f, h * 0.23f, charPaint)
            }

            // 3. Burned-in Subtitles (Hindi/English Devanagari safe via StaticLayout)
            if (request.project.enableSubtitles) {
                val subText = request.scene.subtitleText.ifBlank { request.scene.narrationText }.take(140)
                if (subText.isNotBlank()) {
                    val subWidth = (w * 0.84f).toInt().coerceAtLeast(200)
                    val layout = StaticLayout.Builder.obtain(
                        subText, 0, subText.length, subtitlePaint, subWidth
                    ).setAlignment(Layout.Alignment.ALIGN_CENTER).setMaxLines(3).build()

                    val boxHeight = layout.height + 36f
                    val boxTop = h - pad - boxHeight - 16f
                    val boxRect = RectF((w - subWidth) / 2f - 28f, boxTop, (w + subWidth) / 2f + 28f, boxTop + boxHeight)
                    canvas.drawRoundRect(boxRect, 18f, 18f, subtitleBgPaint)

                    canvas.save()
                    canvas.translate((w - subWidth) / 2f, boxTop + 18f)
                    layout.draw(canvas)
                    canvas.restore()
                }
            }

            // 4. Thin Cinema Timeline Progress Bar at bottom
            canvas.drawRect(0f, h - 8f, w * progress, h, accentPaint)

            // 5. Transition Overlay (Fade / Slide / Zoom)
            when (request.scene.transitionStyle) {
                TransitionStyle.CROSSFADE -> {
                    val alpha = when {
                        progress < 0.12f -> ((1f - progress / 0.12f) * 255f).toInt()
                        progress > 0.88f -> (((progress - 0.88f) / 0.12f) * 255f).toInt()
                        else -> 0
                    }.coerceIn(0, 255)
                    if (alpha > 0) {
                        fadePaint.color = Color.argb(alpha, 8, 10, 18)
                        canvas.drawRect(0f, 0f, w, h, fadePaint)
                    }
                }
                TransitionStyle.SLIDE_LEFT -> {
                    if (progress < 0.12f) {
                        val coverW = w * (1f - progress / 0.12f)
                        fadePaint.color = Color.rgb(11, 15, 26)
                        canvas.drawRect(0f, 0f, coverW, h, fadePaint)
                    }
                }
                TransitionStyle.ZOOM_DISSOLVE -> {
                    if (progress < 0.1f) {
                        val alpha = ((1f - progress / 0.1f) * 180f).toInt().coerceIn(0, 255)
                        fadePaint.color = Color.argb(alpha, 0, 229, 255)
                        canvas.drawRect(0f, 0f, w, h, fadePaint)
                    }
                }
                TransitionStyle.NONE -> Unit
            }
        }
    }

    private fun sceneColorPalette(index: Int): Pair<Int, Int> {
        val palettes = listOf(
            Color.parseColor("#0B132B") to Color.parseColor("#1C2541"),
            Color.parseColor("#161032") to Color.parseColor("#2B193D"),
            Color.parseColor("#0A2239") to Color.parseColor("#1D3557"),
            Color.parseColor("#1E152A") to Color.parseColor("#3B1F2B")
        )
        return palettes[index % palettes.size]
    }

    companion object {
        private const val TAG = "OnDeviceMp4Provider"
    }
}
