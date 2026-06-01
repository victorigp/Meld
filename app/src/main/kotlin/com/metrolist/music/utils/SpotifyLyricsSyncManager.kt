/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Manages push-based synchronization of the current playback state to the
 * SpotifyLyrics web app backend. Sends HTTP POST payloads containing track info,
 * progress, and play state whenever significant player events occur.
 *
 * Implements a 300ms debounce on manual events (seek, rapid play/pause) to
 * prevent flooding the server. A periodic heartbeat sends progress updates
 * every [HEARTBEAT_INTERVAL_MS] while music is playing.
 */
class SpotifyLyricsSyncManager(
    private val scope: CoroutineScope,
    var syncId: String,
    private val endpointUrl: String = DEFAULT_ENDPOINT_URL,
) {
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

    private var debounceJob: Job? = null
    private var heartbeatJob: Job? = null

    // Cached state to avoid sending duplicate payloads
    private var lastMetadata: MediaMetadata? = null
    private var lastIsPlaying: Boolean = false

    // Position supplier — set by MusicService to read current position from the player
    var positionSupplier: (() -> Long)? = null

    // Duration supplier — set by MusicService to read current duration from the player
    var durationSupplier: (() -> Long)? = null

    /**
     * Called when the track changes. Sends an immediate update (debounced).
     */
    fun onTrackChanged(metadata: MediaMetadata?, isPlaying: Boolean) {
        lastMetadata = metadata
        lastIsPlaying = isPlaying
        sendDebounced()
        manageHeartbeat(isPlaying)
    }

    /**
     * Called when play/pause state changes.
     */
    fun onPlayStateChanged(isPlaying: Boolean, metadata: MediaMetadata?) {
        lastMetadata = metadata ?: lastMetadata
        lastIsPlaying = isPlaying
        sendDebounced()
        manageHeartbeat(isPlaying)
    }

    /**
     * Called on seek / position discontinuity.
     */
    fun onSeek(metadata: MediaMetadata?) {
        lastMetadata = metadata ?: lastMetadata
        sendDebounced()
    }

    /**
     * Stops the manager and cancels all pending work.
     */
    fun destroy() {
        debounceJob?.cancel()
        heartbeatJob?.cancel()
        // Send a final "not playing" state so the web client knows playback stopped
        val metadata = lastMetadata ?: return
        val progressMs = positionSupplier?.invoke() ?: 0L
        val durationMs = durationSupplier?.invoke()
        scope.launch(Dispatchers.IO) {
            sendPayload(metadata, false, progressMs, durationMs)
        }
    }

    /**
     * Debounces outgoing requests by [DEBOUNCE_MS]. Rapid-fire events
     * (e.g. dragging the seek bar) only trigger a single POST.
     */
    private fun sendDebounced() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(DEBOUNCE_MS)
            val metadata = lastMetadata ?: return@launch
            val progressMs = positionSupplier?.invoke() ?: 0L
            val durationMs = durationSupplier?.invoke()
            withContext(Dispatchers.IO) {
                sendPayload(metadata, lastIsPlaying, progressMs, durationMs)
            }
        }
    }

    /**
     * Starts or stops the periodic heartbeat that keeps the web client
     * in sync while music is actively playing.
     */
    private fun manageHeartbeat(isPlaying: Boolean) {
        if (isPlaying) {
            // Only start a new heartbeat if one isn't already running
            if (heartbeatJob?.isActive != true) {
                heartbeatJob = scope.launch {
                    while (isActive) {
                        delay(HEARTBEAT_INTERVAL_MS)
                        val metadata = lastMetadata ?: continue
                        if (!lastIsPlaying) break
                        val progressMs = positionSupplier?.invoke() ?: 0L
                        val durationMs = durationSupplier?.invoke()
                        withContext(Dispatchers.IO) {
                            sendPayload(metadata, true, progressMs, durationMs)
                        }
                    }
                }
            }
        } else {
            heartbeatJob?.cancel()
            heartbeatJob = null
        }
    }

    /**
     * Builds and sends the JSON payload to the SpotifyLyrics backend.
     * All network errors are caught silently to never disrupt playback.
     */
    private fun sendPayload(metadata: MediaMetadata, isPlaying: Boolean, progressMs: Long, durationMs: Long?) {
        if (syncId.isBlank()) return

        try {
            val finalDurationMs = durationMs
                ?.takeIf { it > 0 }
                ?: (metadata.duration * 1000L)

            val trackJson = JSONObject().apply {
                put("name", metadata.title)
                put("artist", metadata.artists.joinToString(", ") { it.name })
                put("album", metadata.album?.title ?: "")
                put("duration_ms", finalDurationMs)
            }

            val payload = JSONObject().apply {
                put("syncId", syncId)
                put("isPlaying", isPlaying)
                put("progress_ms", progressMs)
                put("track", trackJson)
                put("timestamp", System.currentTimeMillis())
            }

            val body = payload.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(endpointUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.tag(TAG).w(
                        "SpotifyLyrics sync POST failed: HTTP ${response.code}"
                    )
                } else {
                    Timber.tag(TAG).d(
                        "SpotifyLyrics sync: sent progress=${progressMs}ms " +
                            "isPlaying=$isPlaying track=\"${metadata.title}\""
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "SpotifyLyrics sync: network error (non-fatal)")
        }
    }

    companion object {
        private const val TAG = "SpotifyLyricsSync"
        private const val DEBOUNCE_MS = 300L
        private const val HEARTBEAT_INTERVAL_MS = 5_000L
        private const val DEFAULT_ENDPOINT_URL =
            "https://spotify-lyrics-three.vercel.app/api/meld-sync"
    }
}
