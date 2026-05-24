package com.galaxy.diablo.data

data class Category(
    val name: String,
    val channels: List<Channel>,
    val isSpecial: Boolean = false
) {
    val count: Int get() = channels.size
}
