package com.galaxy.diablo.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** A live TV channel parsed from the playlist. */
@Parcelize
data class Channel(
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val category: String = "Uncategorized",
    /** Optional license URL — set when this channel has DRM. */
    val licenseUrl: String? = null,
    /** Detected DRM scheme: "widevine", "clearkey" or null. */
    val drmScheme: String? = null,
    /** Optional clearkey/widevine raw key (kid:key pair or JWK json). */
    val drmKey: String? = null,
    val userAgent: String? = null,
    val referer: String? = null,
    val origin: String? = null,
    val authorization: String? = null,
    val tvgId: String? = null,
    val tvgLogo: String? = null,
    val manifestType: String? = null
) : Parcelable {

    val hasDrm: Boolean get() = drmScheme != null

    /** Two-letter monogram used as logo fallback (e.g. "AN" for "ANTV"). */
    val monogram: String
        get() {
            val cleaned = name.uppercase().replace(Regex("[^A-Z0-9 ]"), "").trim()
            if (cleaned.isEmpty()) return "TV"
            val words = cleaned.split(Regex("\\s+"))
            return when {
                words.size >= 2 -> (words[0].first().toString() + words[1].first().toString())
                cleaned.length >= 2 -> cleaned.substring(0, 2)
                else -> cleaned
            }
        }

    val streamType: StreamType
        get() {
            val lower = streamUrl.lowercase()
            return when {
                manifestType?.equals("dash", true) == true -> StreamType.DASH
                manifestType?.equals("hls", true) == true -> StreamType.HLS
                lower.contains(".mpd") -> StreamType.DASH
                lower.contains(".m3u8") -> StreamType.HLS
                lower.startsWith("rtsp://") -> StreamType.RTSP
                lower.startsWith("rtmp://") -> StreamType.RTMP
                else -> StreamType.OTHER
            }
        }
}

enum class StreamType { DASH, HLS, RTSP, RTMP, OTHER }
