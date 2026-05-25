package com.galaxy.diablo.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Fetches the playlist from a URL, caches it on disk, and parses it into channels.
 */
class PlaylistRepository(private val context: Context) {

    private val cacheFile: File get() = File(context.cacheDir, "playlist.txt")

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        // No OkHttp cache; we manage our own on-disk cache and want every
        // network call to actually hit the network.
        .cache(null)
        .build()

    /**
     * Appends a `_ts` cache-buster query parameter to defeat any intermediate
     * CDN (e.g. Fastly in front of raw.githubusercontent.com, which honours
     * `cache-control: max-age=300` and would otherwise serve a stale copy for
     * up to 5 minutes after a GitHub push).
     */
    private fun bustCdnCache(url: String): String {
        val separator = if (url.contains("?")) "&" else "?"
        return "$url${separator}_ts=${System.currentTimeMillis()}"
    }

    private fun buildRequest(url: String): Request = Request.Builder()
        .url(bustCdnCache(url))
        .header("User-Agent", "GalaxyDiablo/1.0 (Android)")
        .header("Cache-Control", "no-cache, no-store, max-age=0")
        .header("Pragma", "no-cache")
        .cacheControl(CacheControl.FORCE_NETWORK)
        .build()

    suspend fun load(
        url: String,
        forceRefresh: Boolean = false,
        onProgress: (status: String, progress: Int) -> Unit = { _, _ -> }
    ): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            val raw = when {
                forceRefresh -> fetchAndCache(url, onProgress)
                cacheFile.exists() -> {
                    onProgress("Membaca cache…", 30)
                    cacheFile.readText().also {
                        // Refresh in background next time (current call uses cache)
                    }
                }
                else -> fetchAndCache(url, onProgress)
            }
            onProgress("Mengurai senarai saluran…", 70)
            val channels = PlaylistParser.parse(raw)
            onProgress("Selesai (${channels.size} channels)", 100)
            Result.success(channels)
        } catch (t: Throwable) {
            // Fallback to cache if available
            if (cacheFile.exists()) {
                runCatching {
                    val cached = cacheFile.readText()
                    onProgress("Online gagal — guna cache…", 70)
                    val channels = PlaylistParser.parse(cached)
                    Result.success(channels)
                }.getOrElse { Result.failure(t) }
            } else {
                Result.failure(t)
            }
        }
    }

    private fun fetchAndCache(url: String, onProgress: (String, Int) -> Unit): String {
        onProgress("Mengambil playlist dari sumber…", 10)
        client.newCall(buildRequest(url)).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("HTTP ${response.code} dari $url")
            }
            val body = response.body?.string() ?: throw RuntimeException("Empty body")
            onProgress("Menyimpan ke cache…", 50)
            cacheFile.writeText(body)
            return body
        }
    }

    fun clearCache() { runCatching { cacheFile.delete() } }

    fun hasCache(): Boolean = cacheFile.exists()

    /**
     * Silent background fetch — always hits the network, updates the cache, returns parsed channels.
     * Used for stale-while-revalidate and periodic auto-refresh.
     */
    suspend fun fetchFreshSilently(url: String): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            client.newCall(buildRequest(url)).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure<List<Channel>>(RuntimeException("HTTP ${response.code}"))
                }
                val body = response.body?.string()
                    ?: return@withContext Result.failure<List<Channel>>(RuntimeException("Empty body"))
                cacheFile.writeText(body)
                Result.success(PlaylistParser.parse(body))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
