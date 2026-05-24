package com.galaxy.diablo.data

import android.content.Context
import android.content.SharedPreferences
import com.galaxy.diablo.BuildConfig

object PrefsManager {

    private const val PREFS_NAME = "galaxy_diablo_prefs"
    private const val KEY_PLAYLIST_URL = "playlist_url"
    private const val KEY_SHOW_WELCOME = "show_welcome_toast"
    private const val KEY_FIRST_RUN = "first_run_done"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_LAST_CATEGORY = "last_category"
    private const val KEY_LAST_CHANNEL = "last_channel"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var playlistUrl: String
        get() = prefs.getString(KEY_PLAYLIST_URL, BuildConfig.PLAYLIST_URL) ?: BuildConfig.PLAYLIST_URL
        set(value) = prefs.edit().putString(KEY_PLAYLIST_URL, value).apply()

    val defaultPlaylistUrl: String get() = BuildConfig.PLAYLIST_URL

    /** When true, the welcome toast will be shown every time HomeActivity starts. */
    var showWelcomeToast: Boolean
        get() = prefs.getBoolean(KEY_SHOW_WELCOME, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_WELCOME, value).apply()

    /** Tracks whether the first-time welcome has been shown at least once. */
    var firstRunDone: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN, false)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_RUN, value).apply()

    var favorites: Set<String>
        get() = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_FAVORITES, value).apply()

    fun toggleFavorite(channelName: String): Boolean {
        val current = favorites.toMutableSet()
        val added = if (current.remove(channelName)) false else { current.add(channelName); true }
        favorites = current
        return added
    }

    fun isFavorite(channelName: String): Boolean = favorites.contains(channelName)

    var lastCategory: String?
        get() = prefs.getString(KEY_LAST_CATEGORY, null)
        set(value) = prefs.edit().putString(KEY_LAST_CATEGORY, value).apply()

    var lastChannelName: String?
        get() = prefs.getString(KEY_LAST_CHANNEL, null)
        set(value) = prefs.edit().putString(KEY_LAST_CHANNEL, value).apply()
}
