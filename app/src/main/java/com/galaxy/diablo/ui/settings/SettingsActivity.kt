package com.galaxy.diablo.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.galaxy.diablo.R
import com.galaxy.diablo.data.PlaylistCache
import com.galaxy.diablo.data.PlaylistRepository
import com.galaxy.diablo.data.PrefsManager
import com.galaxy.diablo.ui.enterImmersiveMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        enterImmersiveMode()

        val etUrl = findViewById<EditText>(R.id.et_playlist_url)
        val swToast = findViewById<SwitchCompat>(R.id.sw_welcome_toast)
        val rowClear = findViewById<View>(R.id.row_clear_cache)
        val tvAbout = findViewById<TextView>(R.id.tv_about)
        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val btnSave = findViewById<Button>(R.id.btn_save_url)
        val btnReset = findViewById<Button>(R.id.btn_reset_url)
        val tvSource = findViewById<TextView>(R.id.tv_source_value)
        val btnToggleSource = findViewById<ImageButton>(R.id.btn_toggle_source)

        // Privacy: do NOT pre-fill the input with the current URL.
        // The current URL is only revealed when the user explicitly taps the eye icon.
        etUrl.setText("")
        var sourceRevealed = false
        tvSource.text = getString(R.string.source_hidden)
        btnToggleSource.setOnClickListener {
            sourceRevealed = !sourceRevealed
            tvSource.text = if (sourceRevealed) PrefsManager.playlistUrl
                else getString(R.string.source_hidden)
        }
        swToast.isChecked = PrefsManager.showWelcomeToast
        tvAbout.text = getString(R.string.about_body)
        tvAbout.movementMethod = LinkMovementMethod.getInstance()

        swToast.setOnCheckedChangeListener { _, checked ->
            PrefsManager.showWelcomeToast = checked
        }
        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val url = etUrl.text.toString().trim()
            if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) {
                Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            PrefsManager.playlistUrl = url
            reloadPlaylist()
        }
        btnReset.setOnClickListener {
            PrefsManager.playlistUrl = PrefsManager.defaultPlaylistUrl
            etUrl.setText("")
            if (sourceRevealed) tvSource.text = PrefsManager.playlistUrl
            reloadPlaylist()
        }
        rowClear.setOnClickListener {
            PlaylistRepository(applicationContext).clearCache()
            Toast.makeText(this, R.string.cache_cleared, Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.row_telegram).setOnClickListener {
            openExternal(getString(R.string.telegram_url))
        }
    }

    private fun openExternal(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Throwable) {
            Toast.makeText(this, R.string.cannot_open_link, Toast.LENGTH_SHORT).show()
        }
    }

    private fun reloadPlaylist() {
        Toast.makeText(this, R.string.refreshing, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val repo = PlaylistRepository(applicationContext)
            val res = repo.load(PrefsManager.playlistUrl, forceRefresh = true) { _, _ -> }
            withContext(Dispatchers.Main) {
                res.fold(
                    onSuccess = {
                        PlaylistCache.channels = it
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.refresh_done, it.size),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailure = {
                        Toast.makeText(
                            this@SettingsActivity,
                            getString(R.string.fetch_failed, it.message ?: ""),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }
}
