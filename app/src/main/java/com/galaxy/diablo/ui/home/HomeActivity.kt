package com.galaxy.diablo.ui.home

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.galaxy.diablo.R
import com.galaxy.diablo.data.Category
import com.galaxy.diablo.data.Channel
import com.galaxy.diablo.data.PlaylistCache
import com.galaxy.diablo.data.PlaylistRepository
import com.galaxy.diablo.data.PrefsManager
import com.galaxy.diablo.ui.player.PlayerActivity
import com.galaxy.diablo.ui.settings.SettingsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var rvChannels: RecyclerView
    private lateinit var emptyState: View
    private lateinit var pbLoading: View
    private lateinit var tvCount: TextView
    private lateinit var searchBar: View
    private lateinit var etSearch: EditText
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var channelAdapter: ChannelAdapter

    private var allChannels: List<Channel> = emptyList()
    private var categories: List<Category> = emptyList()
    private var query: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        rvCategories = findViewById(R.id.rv_categories)
        rvChannels = findViewById(R.id.rv_channels)
        emptyState = findViewById(R.id.empty_state)
        pbLoading = findViewById(R.id.pb_loading_grid)
        tvCount = findViewById(R.id.tv_channel_count)
        searchBar = findViewById(R.id.search_bar)
        etSearch = findViewById(R.id.et_search)

        setupRecyclers()
        wireTopBar()
        wireSearch()

        allChannels = PlaylistCache.channels
        rebuildCategories()
        showCurrentCategoryChannels()
        maybeShowWelcomeToast()
    }

    private fun setupRecyclers() {
        rvCategories.layoutManager = LinearLayoutManager(this)
        categoryAdapter = CategoryAdapter(emptyList()) { _ ->
            showCurrentCategoryChannels()
        }
        rvCategories.adapter = categoryAdapter

        rvChannels.layoutManager = GridLayoutManager(this, 6)
        channelAdapter = ChannelAdapter(emptyList()) { channel, position ->
            openPlayer(channel, position)
        }
        rvChannels.adapter = channelAdapter
    }

    private fun wireTopBar() {
        findViewById<ImageButton>(R.id.btn_search).setOnClickListener {
            val visible = searchBar.visibility == View.VISIBLE
            searchBar.visibility = if (visible) View.GONE else View.VISIBLE
            if (!visible) etSearch.requestFocus()
        }
        findViewById<ImageButton>(R.id.btn_search_close).setOnClickListener {
            etSearch.setText("")
            searchBar.visibility = View.GONE
        }
        findViewById<ImageButton>(R.id.btn_refresh).setOnClickListener { refreshPlaylist(force = true) }
        findViewById<ImageButton>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btn_clear_cache).setOnClickListener { confirmClearCache() }
        findViewById<ImageButton>(R.id.btn_exit).setOnClickListener { confirmExit() }
    }

    private fun wireSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString().orEmpty().trim()
                showCurrentCategoryChannels()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun rebuildCategories() {
        val grouped = allChannels.groupBy { it.category }
        val natural = grouped.map { (name, list) -> Category(name, list) }
            .sortedBy { it.name.lowercase() }
        val favorites = allChannels.filter { PrefsManager.isFavorite(it.name) }
        val all = Category("Semua", allChannels, isSpecial = true)
        val fav = Category("Kegemaran", favorites, isSpecial = true)
        categories = mutableListOf<Category>().apply {
            add(all)
            if (favorites.isNotEmpty()) add(fav)
            addAll(natural)
        }
        categoryAdapter.submit(categories, keepSelection = true)
        tvCount.text = allChannels.size.toString()
    }

    private fun showCurrentCategoryChannels() {
        val cat = categoryAdapter.selected() ?: return
        var list = cat.channels
        if (query.isNotEmpty()) {
            list = list.filter { it.name.contains(query, ignoreCase = true) }
        }
        channelAdapter.submit(list)
        emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        rvChannels.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun openPlayer(channel: Channel, position: Int) {
        val cat = categoryAdapter.selected() ?: return
        PlayerActivity.start(this, cat.channels, channel)
    }

    private fun refreshPlaylist(force: Boolean) {
        pbLoading.visibility = View.VISIBLE
        lifecycleScope.launch {
            val repo = PlaylistRepository(applicationContext)
            val res = repo.load(PrefsManager.playlistUrl, forceRefresh = force) { _, _ -> }
            withContext(Dispatchers.Main) {
                pbLoading.visibility = View.GONE
                res.fold(
                    onSuccess = {
                        PlaylistCache.channels = it
                        allChannels = it
                        rebuildCategories()
                        showCurrentCategoryChannels()
                        toast(getString(R.string.refresh_done, it.size))
                    },
                    onFailure = { toast(getString(R.string.fetch_failed, it.message ?: "")) }
                )
            }
        }
    }

    private fun confirmClearCache() {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_clear_cache)
            .setMessage(R.string.confirm_clear_cache)
            .setPositiveButton(R.string.yes) { _, _ ->
                PlaylistRepository(applicationContext).clearCache()
                refreshPlaylist(force = true)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmExit() {
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_exit_title)
            .setMessage(R.string.confirm_exit_message)
            .setPositiveButton(R.string.yes) { _, _ -> finishAffinity() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun maybeShowWelcomeToast() {
        if (!PrefsManager.showWelcomeToast && PrefsManager.firstRunDone) return
        // First-time toast = always shown. Subsequent shows depend on toggle.
        showGalaxyToast()
        PrefsManager.firstRunDone = true
    }

    private fun showGalaxyToast() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_toast)
            gravity = Gravity.CENTER
        }
        val title = TextView(this).apply {
            text = getString(R.string.welcome_toast_title)
            setTextColor(0xFF67E8F9.toInt())
            textSize = 14f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val body = TextView(this).apply {
            text = getString(R.string.welcome_toast)
            setTextColor(0xFFE6F7FF.toInt())
            textSize = 12f
            gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (resources.displayMetrics.density * 6).toInt() }
            layoutParams = lp
        }
        container.addView(title)
        container.addView(body)

        Toast(this).apply {
            duration = Toast.LENGTH_LONG
            setGravity(Gravity.CENTER_HORIZONTAL or Gravity.TOP, 0, (resources.displayMetrics.density * 80).toInt())
            @Suppress("DEPRECATION")
            view = container
            show()
        }
    }

    private fun toast(msg: String) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }

    override fun onResume() {
        super.onResume()
        if (PlaylistCache.channels !== allChannels) {
            allChannels = PlaylistCache.channels
            rebuildCategories()
            showCurrentCategoryChannels()
        }
    }
}
