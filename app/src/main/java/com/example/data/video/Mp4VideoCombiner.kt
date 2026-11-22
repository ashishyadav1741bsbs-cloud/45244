package com.example.data.video

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Combines multiple scene MP4 files into one continuous MP4 using MediaExtractor + MediaMuxer.
 * Optionally muxes a user-selected background audio track (AAC/MP4/M4A/MP3-if-muxable) cleanly.
 */
class Mp4VideoCombiner(private val context: Context) {

    suspend fun combineScenes(
        clipFiles: List<File>,
        outputFile: File,
        backgroundMusicUri: String?,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val validClips = clipFiles.filter { it.exists() && it.length() > 0L }
            require(validClips.isNotEmpty()) {
                "No rendered scene clips available to combine."
            }

            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) outputFile.delete()

            // Determine video & audio format from the first valid clip
            val firstExtractor = MediaExtractor()
            firstExtractor.setDataSource(validClips.first().absolutePath)
            var videoFormat: MediaFormat? = null
            var audioFormat: MediaFormat? = null
            for (i in 0 until firstExtractor.trackCount) {
                val fmt = firstExtractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("video/") && videoFormat == null) videoFormat = fmt
                if (mime.startsWith("audio/") && audioFormat == null) audioFormat = fmt
            }
            firstExtractor.release()

            val resolvedVideoFormat = videoFormat ?: error("First scene clip has no video track.")

            // Check if user picked background music that MediaMuxer supports directly (AAC/M4A)
            val bgMusicFormat = backgroundMusicUri?.let { extractSupportedAudioFormat(it) }

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outVideoTrack = muxer.addTrack(resolvedVideoFormat)
            val useBgMusicTrack = bgMusicFormat != null && audioFormat == null
            val activeAudioFormat = audioFormat ?: if (useBgMusicTrack) bgMusicFormat else null
            val outAudioTrack = activeAudioFormat?.let {
                runCatching { muxer.addTrack(it) }.getOrNull()
            } ?: -1

            muxer.start()
            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            var videoPtsOffsetUs = 0L
            var audioPtsOffsetUs = 0L

            try {
                validClips.forEachIndexed { index, clip ->
                    currentCoroutineContext().ensureActive()
                    val extractor = MediaExtractor()
                    extractor.setDataSource(clip.absolutePath)

                    var srcVideoTrack = -1
                    var srcAudioTrack = -1
                    var clipDurationUs = 0L

                    for (t in 0 until extractor.trackCount) {
                        val fmt = extractor.getTrackFormat(t)
                        val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                        if (mime.startsWith("video/") && srcVideoTrack == -1) {
                            srcVideoTrack = t
                            if (fmt.containsKey(MediaFormat.KEY_DURATION)) {
                                clipDurationUs = max(clipDurationUs, fmt.getLong(MediaFormat.KEY_DURATION))
                            }
                        } else if (mime.startsWith("audio/") && srcAudioTrack == -1) {
                            srcAudioTrack = t
                        }
                    }

                    var maxSampleVideoPtsUs = 0L
                    if (srcVideoTrack >= 0) {
                        extractor.selectTrack(srcVideoTrack)
                        while (true) {
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) break
                            val samplePts = extractor.sampleTime.coerceAtLeast(0L)
                            maxSampleVideoPtsUs = max(maxSampleVideoPtsUs, samplePts)
                            bufferInfo.offset = 0
                            bufferInfo.size = sampleSize
                            bufferInfo.presentationTimeUs = videoPtsOffsetUs + samplePts
                            bufferInfo.flags = if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                                MediaCodec.BUFFER_FLAG_KEY_FRAME
                            } else 0
                            muxer.writeSampleData(outVideoTrack, buffer, bufferInfo)
                            extractor.advance()
                        }
                        extractor.unselectTrack(srcVideoTrack)
                    }

                    var maxSampleAudioPtsUs = 0L
                    if (srcAudioTrack >= 0 && outAudioTrack >= 0 && !useBgMusicTrack) {
                        extractor.selectTrack(srcAudioTrack)
                        while (true) {
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) break
                            val samplePts = extractor.sampleTime.coerceAtLeast(0L)
                            maxSampleAudioPtsUs = max(maxSampleAudioPtsUs, samplePts)
                            bufferInfo.offset = 0
                            bufferInfo.size = sampleSize
                            bufferInfo.presentationTimeUs = audioPtsOffsetUs + samplePts
                            bufferInfo.flags = 0
                            muxer.writeSampleData(outAudioTrack, buffer, bufferInfo)
                            extractor.advance()
                        }
                        extractor.unselectTrack(srcAudioTrack)
                    }

                    val stepUs = max(clipDurationUs, max(maxSampleVideoPtsUs, maxSampleAudioPtsUs)) + 50_000L
                    videoPtsOffsetUs += stepUs
                    audioPtsOffsetUs += stepUs
                    extractor.release()
                    onProgress((index + 1).toFloat() / validClips.size.toFloat())
                }

                // Append background music across the combined video duration if selected & compatible
                if (useBgMusicTrack && outAudioTrack >= 0 && backgroundMusicUri != null) {
                    appendLoopedBackgroundMusic(
                        uriString = backgroundMusicUri,
                        maxDurationUs = videoPtsOffsetUs,
                        muxer = muxer,
                        outAudioTrack = outAudioTrack,
                        buffer = buffer,
                        bufferInfo = bufferInfo
                    )
                }
            } finally {
                runCatching { muxer.stop() }
                runCatching { muxer.release() }
            }

            check(outputFile.exists() && outputFile.length() > 0L) {
                "Combined MP4 output is empty."
            }
            outputFile
        }
    }

    private fun extractSupportedAudioFormat(uriString: String): MediaFormat? {
        return runCatching {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, Uri.parse(uriString), null)
            var found: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime == MediaFormat.MIMETYPE_AUDIO_AAC) {
                    found = fmt
                    break
                }
            }
            extractor.release()
            found
        }.getOrNull()
    }

    private fun appendLoopedBackgroundMusic(
        uriString: String,
        maxDurationUs: Long,
        muxer: MediaMuxer,
        outAudioTrack: Int,
        buffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        runCatching {
            var currentOffsetUs = 0L
            while (currentOffsetUs < maxDurationUs) {
                val extractor = MediaExtractor()
                extractor.setDataSource(context, Uri.parse(uriString), null)
                val trackIdx = (0 until extractor.trackCount).firstOrNull {
                    extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME) == MediaFormat.MIMETYPE_AUDIO_AAC
                }
                if (trackIdx == null) {
                    extractor.release()
                    break
                }
                extractor.selectTrack(trackIdx)
                var lastPts = 0L
                while (true) {
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break
                    val pts = extractor.sampleTime.coerceAtLeast(0L)
                    if (currentOffsetUs + pts >= maxDurationUs) break
                    lastPts = pts
                    bufferInfo.offset = 0
                    bufferInfo.size = size
                    bufferInfo.presentationTimeUs = currentOffsetUs + pts
                    bufferInfo.flags = 0
                    muxer.writeSampleData(outAudioTrack, buffer, bufferInfo)
                    extractor.advance()
                }
                extractor.release()
                if (lastPts <= 0L) break
                currentOffsetUs += lastPts + 23_000L
            }
        }.onFailure {
            Log.w("Mp4VideoCombiner", "Skipped incompatible background music format: ${it.message}")
        }
    }
}
