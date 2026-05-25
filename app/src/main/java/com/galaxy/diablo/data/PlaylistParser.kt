package com.galaxy.diablo.data

/**
 * Parses both standard M3U playlists (with #EXTM3U / #EXTINF / #KODIPROP / #EXTVLCOPT)
 * AND the custom "animedantv" format used in `hairunnizam21/myiptv-playlist`:
 *
 *     === Category Name ===
 *
 *     Channel Name
 *     https://logo.png
 *     https://stream.mpd
 *     https://license.url   ← optional
 *
 *     Another Channel
 *     ...
 *
 * The parser auto-detects the format based on the file contents.
 */
object PlaylistParser {

    fun parse(content: String): List<Channel> {
        val text = content.trim()
        val isStandardM3u = text.startsWith("#EXTM3U") ||
            text.lineSequence().take(5).any { it.startsWith("#EXTINF") }
        return if (isStandardM3u) parseStandard(text) else parseAnimedantv(text)
    }

    // -----------------------------------------------------------------------
    // animedantv custom format
    // -----------------------------------------------------------------------

    private val CATEGORY_REGEX = Regex("^=+\\s*(.+?)\\s*=+\$")

    private fun parseAnimedantv(text: String): List<Channel> {
        val out = mutableListOf<Channel>()
        // Split into blocks separated by blank lines
        val lines = text.lines()
        var currentCategory = "Lain-lain"
        var buffer = mutableListOf<String>()

        fun flush() {
            if (buffer.isEmpty()) { return }
            val name = buffer[0].trim()
            val urls = buffer.drop(1).map { it.trim() }.filter { isHttp(it) }
            if (name.isNotEmpty() && urls.isNotEmpty()) {
                val logo: String?
                val stream: String?
                val license: String?
                when (urls.size) {
                    1 -> { logo = null; stream = urls[0]; license = null }
                    2 -> {
                        if (looksLikeImage(urls[0])) { logo = urls[0]; stream = urls[1]; license = null }
                        else { logo = null; stream = urls[0]; license = urls[1] }
                    }
                    else -> {
                        // 3 or more: assume [logo, stream, license, ...]
                        if (looksLikeImage(urls[0])) {
                            logo = urls[0]; stream = urls[1]; license = urls.getOrNull(2)
                        } else {
                            logo = null; stream = urls[0]; license = urls.getOrNull(1)
                        }
                    }
                }
                if (stream != null) {
                    val drm = detectDrm(stream, license)
                    out += Channel(
                        name = name,
                        streamUrl = stream,
                        logoUrl = logo,
                        category = currentCategory,
                        licenseUrl = license,
                        drmScheme = drm
                    )
                }
            }
            buffer = mutableListOf()
        }

        for (raw in lines) {
            val line = raw.trim()
            val catMatch = CATEGORY_REGEX.matchEntire(line)
            if (catMatch != null) {
                flush()
                currentCategory = catMatch.groupValues[1].trim()
                continue
            }
            if (line.isEmpty()) {
                flush()
                continue
            }
            buffer += line
        }
        flush()
        return out
    }

    private fun isHttp(s: String) =
        s.startsWith("http://", true) || s.startsWith("https://", true) ||
            s.startsWith("rtmp://", true) || s.startsWith("rtsp://", true)

    private fun looksLikeImage(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png") ||
            lower.contains(".webp") || lower.contains(".gif") || lower.contains(".svg") ||
            lower.contains(".ico")
    }

    /**
     * Heuristic DRM detection.
     *
     * Returns the DRM scheme name, or null when the stream has no DRM or when the
     * supplied license cannot be applied to the stream (e.g. an inline `kid:key`
     * supplied alongside an HLS stream — ExoPlayer does not support inline CENC
     * ClearKey for HLS, so we drop the DRM rather than fail the playback init).
     */
    private fun detectDrm(stream: String, license: String?): String? {
        if (license.isNullOrBlank()) return null
        val s = stream.lowercase()
        val l = license.lowercase()
        val isHls = s.contains(".m3u8")
        val isDash = s.contains(".mpd")
        val isInlineKeyPair = !l.startsWith("http") && l.contains(":") && l.length < 200
        val isClearKeyProxyUrl = l.startsWith("http") && (
            l.contains("clearkey") || l.contains("cumbudrm") || l.contains("semar.my.id") ||
                l.contains("cumbu") || l.contains("getkey") || l.contains("drm.php") ||
                l.contains("mod_chk") || l.contains("type=clearkey")
        )
        val isExplicitWidevine = l.contains("widevine") || l.contains("type=widevine")

        return when {
            // Inline `kid:key` next to an HLS stream cannot be applied — skip DRM.
            isInlineKeyPair && isHls -> null
            isInlineKeyPair -> "clearkey"
            isExplicitWidevine -> "widevine"
            isClearKeyProxyUrl -> "clearkey"
            isDash && l.startsWith("http") -> "widevine"
            else -> null
        }
    }

    // -----------------------------------------------------------------------
    // Standard M3U format
    // -----------------------------------------------------------------------

    private val ATTR_REGEX = Regex("(\\S+?)=\"([^\"]*?)\"")

    private fun parseStandard(text: String): List<Channel> {
        val out = mutableListOf<Channel>()
        val lines = text.lines().map { it.trim() }
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (!line.startsWith("#EXTINF", true)) { i++; continue }

            val attrs = mutableMapOf<String, String>()
            for (m in ATTR_REGEX.findAll(line)) {
                attrs[m.groupValues[1].lowercase()] = m.groupValues[2]
            }
            val name = line.substringAfter(",", "").trim()

            i++
            var drmScheme: String? = null
            var drmKey: String? = null
            var manifestType: String? = null
            var userAgent: String? = null
            var referer: String? = null
            var origin: String? = null
            var authorization: String? = null
            var url: String? = null

            while (i < lines.size) {
                val l = lines[i]
                when {
                    l.startsWith("#KODIPROP:", true) -> {
                        val body = l.substringAfter(":").trim()
                        when {
                            body.startsWith("inputstream.adaptive.license_type=", true) -> {
                                val lt = body.substringAfter("=").lowercase()
                                drmScheme = when {
                                    lt.contains("widevine") -> "widevine"
                                    lt.contains("clearkey") -> "clearkey"
                                    else -> drmScheme
                                }
                            }
                            body.startsWith("inputstream.adaptive.license_key=", true) ->
                                drmKey = body.substringAfter("=").trim()
                            body.startsWith("inputstream.adaptive.manifest_type=", true) ->
                                manifestType = body.substringAfter("=").trim().lowercase()
                        }
                    }
                    l.startsWith("#EXTVLCOPT:", true) -> {
                        val body = l.substringAfter(":").trim()
                        val (k, v) = body.split("=", limit = 2).let {
                            if (it.size == 2) it[0].lowercase() to it[1].trim() else "" to ""
                        }
                        when (k) {
                            "http-user-agent", "user-agent" -> userAgent = v
                            "http-referrer", "http-referer", "referer", "referrer" -> referer = v
                            "http-origin", "origin" -> origin = v
                            "http-authorization", "authorization" -> authorization = v
                        }
                    }
                    l.startsWith("#EXTHTTP:", true) -> {
                        /* JSON headers — ignore for now */
                    }
                    l.startsWith("#") -> { /* ignored */ }
                    l.isBlank() -> { /* skip */ }
                    isHttp(l) -> { url = l; i++; break }
                }
                i++
            }

            if (!url.isNullOrBlank()) {
                val category = attrs["group-title"]?.takeIf { it.isNotBlank() } ?: "Uncategorized"
                out += Channel(
                    name = name.ifBlank { attrs["tvg-name"] ?: "Unknown" },
                    streamUrl = url,
                    logoUrl = attrs["tvg-logo"],
                    category = category,
                    drmScheme = drmScheme,
                    drmKey = drmKey,
                    manifestType = manifestType,
                    userAgent = userAgent,
                    referer = referer,
                    origin = origin,
                    authorization = authorization,
                    tvgId = attrs["tvg-id"],
                    tvgLogo = attrs["tvg-logo"]
                )
            }
        }
        return out
    }
}
