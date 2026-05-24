package com.galaxy.diablo.ui.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.galaxy.diablo.R
import com.galaxy.diablo.data.Channel
import com.galaxy.diablo.ui.enterImmersiveMode
import org.json.JSONObject

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var loadingOverlay: View
    private lateinit var tvLoading: TextView
    private var player: ExoPlayer? = null

    private var channels: List<Channel> = emptyList()
    private var currentIndex: Int = 0
    private val ratios = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "FIT",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "FILL",
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "ZOOM",
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH to "FIXED W",
        AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT to "FIXED H"
    )
    private var ratioIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = Color.BLACK
        setContentView(R.layout.activity_player)
        enterImmersiveMode()

        playerView = findViewById(R.id.player_view)
        loadingOverlay = findViewById(R.id.loading_overlay)
        tvLoading = findViewById(R.id.tv_loading_label)

        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        channels = (intent.getParcelableArrayListExtra<Channel>(EXTRA_PLAYLIST))?.toList()
            ?: emptyList()
        currentIndex = channels.indexOfFirst {
            it.streamUrl == intent.getStringExtra(EXTRA_CURRENT_URL)
        }.coerceAtLeast(0)

        if (channels.isEmpty()) {
            Toast.makeText(this, R.string.no_channels, Toast.LENGTH_SHORT).show()
            finish(); return
        }

        wireControls()
        initPlayer()
        playCurrent()
    }

    private fun wireControls() {
        playerView.findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener { finish() }
        playerView.findViewById<ImageButton>(R.id.btn_prev_channel)?.setOnClickListener { previous() }
        playerView.findViewById<ImageButton>(R.id.btn_next_channel)?.setOnClickListener { next() }
        playerView.findViewById<ImageButton>(R.id.btn_pip)?.setOnClickListener { enterPip() }
        playerView.findViewById<ImageButton>(R.id.btn_video)?.setOnClickListener { showTrackPicker(C.TRACK_TYPE_VIDEO) }
        playerView.findViewById<ImageButton>(R.id.btn_audio)?.setOnClickListener { showTrackPicker(C.TRACK_TYPE_AUDIO) }
        playerView.findViewById<ImageButton>(R.id.btn_subtitle)?.setOnClickListener { showTrackPicker(C.TRACK_TYPE_TEXT) }
        playerView.findViewById<ImageButton>(R.id.btn_ratio)?.setOnClickListener { cycleAspectRatio() }
    }

    private fun initPlayer() {
        val p = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        playerView.player = p
        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                loadingOverlay.visibility = if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) loadingOverlay.visibility = View.GONE
            }

            override fun onPlayerError(error: PlaybackException) {
                loadingOverlay.visibility = View.GONE
                val ch = channels.getOrNull(currentIndex)
                val msg = buildString {
                    append(ch?.name ?: "Channel")
                    append(" • ")
                    append(error.errorCodeName)
                    error.cause?.message?.let { append(" • ").append(it.take(80)) }
                }
                Toast.makeText(this@PlayerActivity, msg, Toast.LENGTH_LONG).show()
            }
        })
        if (BuildFlags.DEBUG_PLAYER) p.addAnalyticsListener(EventLogger())
        player = p
    }

    private fun playCurrent() {
        val ch = channels.getOrNull(currentIndex) ?: return
        val titleView = playerView.findViewById<TextView>(R.id.tv_title)
        titleView?.text = ch.name
        loadingOverlay.visibility = View.VISIBLE

        // Many IPTV CDNs reject the default ExoPlayer UA; mimic a modern Chrome on Android.
        val defaultUa = "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(ch.userAgent ?: defaultUa)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(20_000)
            .apply {
                val headers = mutableMapOf<String, String>()
                ch.referer?.let { headers["Referer"] = it }
                ch.origin?.let { headers["Origin"] = it }
                ch.authorization?.let { headers["Authorization"] = it }
                if (headers.isNotEmpty()) setDefaultRequestProperties(headers)
            }

        val mediaItemBuilder = MediaItem.Builder().setUri(ch.streamUrl)
        val lowerUrl = ch.streamUrl.lowercase()
        when (ch.streamType) {
            com.galaxy.diablo.data.StreamType.DASH ->
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
            com.galaxy.diablo.data.StreamType.HLS ->
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            com.galaxy.diablo.data.StreamType.RTSP ->
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_RTSP)
            else -> {
                // Heuristic: many IPTV stream URLs don't have file extensions.
                // Try to nudge ExoPlayer based on query strings or path hints.
                when {
                    lowerUrl.contains("m3u8") -> mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                    lowerUrl.contains(".mpd") || lowerUrl.contains("manifest.mpd") ->
                        mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
                    lowerUrl.contains(".ts") -> mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP2T)
                    lowerUrl.contains(".mp4") -> mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4)
                    else -> { /* let DefaultMediaSourceFactory auto-detect */ }
                }
            }
        }

        // DRM configuration
        if (ch.hasDrm) {
            val drmScheme = when (ch.drmScheme?.lowercase()) {
                "clearkey" -> C.CLEARKEY_UUID
                else -> C.WIDEVINE_UUID
            }
            val drmConfigBuilder = MediaItem.DrmConfiguration.Builder(drmScheme)
            when {
                // ClearKey can be either a license server URL or a key map (kid:key or jwks)
                ch.drmScheme.equals("clearkey", true) -> {
                    val key = ch.drmKey ?: ch.licenseUrl
                    if (!key.isNullOrBlank() && !key.startsWith("http")) {
                        val jwks = clearKeyToJwks(key)
                        drmConfigBuilder.setKeySetId(null)
                        // Use license URL with data: scheme is not supported; we use license response override via license URI of "data:"-style is not supported.
                        // Workaround: set keySetId is for persistence; we instead provide via license URI hack:
                        // The recommended way is to set licenseUri = "data:application/json;base64,<base64 jwks>"
                        val b64 = android.util.Base64.encodeToString(
                            jwks.toByteArray(),
                            android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE
                        )
                        drmConfigBuilder.setLicenseUri("data:application/json;base64,$b64")
                    } else if (!ch.licenseUrl.isNullOrBlank()) {
                        drmConfigBuilder.setLicenseUri(ch.licenseUrl)
                    }
                }
                else -> {
                    if (!ch.licenseUrl.isNullOrBlank()) drmConfigBuilder.setLicenseUri(ch.licenseUrl)
                }
            }
            mediaItemBuilder.setDrmConfiguration(drmConfigBuilder.build())
        }

        val sourceFactory: MediaSource.Factory = when (ch.streamType) {
            com.galaxy.diablo.data.StreamType.DASH -> DashMediaSource.Factory(httpFactory)
            com.galaxy.diablo.data.StreamType.HLS -> HlsMediaSource.Factory(httpFactory)
            else -> DefaultMediaSourceFactory(this).setDataSourceFactory(httpFactory)
        }
        val drmProvider = DefaultDrmSessionManagerProvider().apply {
            setDrmHttpDataSourceFactory(httpFactory)
        }
        if (sourceFactory is DashMediaSource.Factory) sourceFactory.setDrmSessionManagerProvider(drmProvider)
        if (sourceFactory is HlsMediaSource.Factory) sourceFactory.setDrmSessionManagerProvider(drmProvider)
        if (sourceFactory is DefaultMediaSourceFactory) sourceFactory.setDrmSessionManagerProvider(drmProvider)

        val mediaItem = mediaItemBuilder.build()
        val source = sourceFactory.createMediaSource(mediaItem)

        val p = player ?: return
        p.setMediaSource(source)
        p.prepare()
        p.playWhenReady = true
    }

    /** Convert a "kid:key" pair (hex, base16 or base64url) into ClearKey JWKS. */
    private fun clearKeyToJwks(raw: String): String {
        val parts = raw.split(":", limit = 2)
        if (parts.size != 2) return """{"keys":[]}"""
        val kid = parts[0].trim()
        val key = parts[1].trim()
        fun toB64Url(s: String): String {
            // Accept hex (32 chars), or base64url, or base64
            val asHex = s.matches(Regex("^[A-Fa-f0-9]{32}$"))
            val bytes = if (asHex) hexToBytes(s) else android.util.Base64.decode(
                s.replace('-', '+').replace('_', '/'),
                android.util.Base64.NO_WRAP
            )
            return android.util.Base64.encodeToString(
                bytes,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
            )
        }
        val kidB64 = toB64Url(kid)
        val keyB64 = toB64Url(key)
        return JSONObject().apply {
            put("keys", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("kty", "oct")
                    put("kid", kidB64)
                    put("k", keyB64)
                })
            })
            put("type", "temporary")
        }.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            out[i] = ((Character.digit(hex[i * 2], 16) shl 4) +
                Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
        return out
    }

    private fun previous() {
        if (channels.isEmpty()) return
        currentIndex = (currentIndex - 1 + channels.size) % channels.size
        playCurrent()
    }

    private fun next() {
        if (channels.isEmpty()) return
        currentIndex = (currentIndex + 1) % channels.size
        playCurrent()
    }

    private fun cycleAspectRatio() {
        ratioIndex = (ratioIndex + 1) % ratios.size
        playerView.resizeMode = ratios[ratioIndex].first
        Toast.makeText(this, ratios[ratioIndex].second, Toast.LENGTH_SHORT).show()
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        } else {
            Toast.makeText(this, "PiP tidak disokong", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPictureInPictureModeChanged(isInPip: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPip, newConfig)
        playerView.useController = !isInPip
    }

    private fun showTrackPicker(trackType: Int) {
        val p = player ?: return
        val groups = p.currentTracks.groups.filter { it.type == trackType }
        if (groups.isEmpty()) {
            val msg = when (trackType) {
                C.TRACK_TYPE_AUDIO -> "Tiada audio track lain"
                C.TRACK_TYPE_TEXT -> "Tiada sarikata tersedia"
                else -> "Tiada video track lain"
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); return
        }
        val items = mutableListOf<Pair<String, Pair<Tracks.Group, Int>>?>()
        if (trackType == C.TRACK_TYPE_TEXT) items += "Mati" to (groups.first() to -1)
        groups.forEach { g ->
            for (i in 0 until g.length) {
                val f = g.getTrackFormat(i)
                val label = when (trackType) {
                    C.TRACK_TYPE_AUDIO -> "${f.language ?: "und"}  ${(f.bitrate / 1000).coerceAtLeast(0)} kbps"
                    C.TRACK_TYPE_VIDEO -> "${f.width}x${f.height}  ${(f.bitrate / 1000).coerceAtLeast(0)} kbps"
                    C.TRACK_TYPE_TEXT -> f.language ?: f.label ?: "Subtitle ${i + 1}"
                    else -> "Track $i"
                }
                items += label to (g to i)
            }
        }
        val labels = items.map { it!!.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(when (trackType) {
                C.TRACK_TYPE_AUDIO -> "Pilih audio"
                C.TRACK_TYPE_TEXT -> "Pilih sarikata"
                else -> "Pilih video"
            })
            .setItems(labels) { _, which ->
                val pick = items[which]!!.second
                val params = p.trackSelectionParameters.buildUpon()
                if (pick.second < 0) {
                    params.setTrackTypeDisabled(trackType, true)
                } else {
                    params.setTrackTypeDisabled(trackType, false)
                    params.setOverrideForType(TrackSelectionOverride(pick.first.mediaTrackGroup, listOf(pick.second)))
                }
                p.trackSelectionParameters = params.build()
            }
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_CHANNEL_DOWN -> { previous(); true }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_CHANNEL_UP -> { next(); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onPause() {
        super.onPause()
        player?.playWhenReady = false
    }

    override fun onResume() {
        super.onResume()
        player?.playWhenReady = true
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_PLAYLIST = "playlist"
        private const val EXTRA_CURRENT_URL = "current_url"

        fun start(context: Context, channels: List<Channel>, current: Channel) {
            val i = Intent(context, PlayerActivity::class.java)
            i.putParcelableArrayListExtra(EXTRA_PLAYLIST, ArrayList(channels))
            i.putExtra(EXTRA_CURRENT_URL, current.streamUrl)
            context.startActivity(i)
        }
    }
}

object BuildFlags {
    const val DEBUG_PLAYER: Boolean = false
}
