package com.galaxy.diablo.data

/**
 * In-memory cache of the currently-loaded channels, populated by SplashActivity
 * and consumed by HomeActivity / PlayerActivity. Avoids re-parsing on every screen.
 */
object PlaylistCache {
    var channels: List<Channel> = emptyList()
}
