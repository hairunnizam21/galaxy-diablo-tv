package com.galaxy.diablo.ui.splash

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.galaxy.diablo.R
import com.galaxy.diablo.data.PlaylistCache
import com.galaxy.diablo.data.PlaylistRepository
import com.galaxy.diablo.data.PrefsManager
import com.galaxy.diablo.ui.home.HomeActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    private lateinit var pb: ProgressBar
    private lateinit var status: TextView
    private lateinit var logo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_splash)

        pb = findViewById(R.id.pb_splash)
        status = findViewById(R.id.tv_splash_status)
        logo = findViewById(R.id.iv_splash_logo)

        animateLogo()
        startLoading()
    }

    private fun animateLogo() {
        ObjectAnimator.ofFloat(logo, "rotation", 0f, 360f).apply {
            duration = 12_000L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(logo, "scaleX", 0.92f, 1.0f, 0.92f).apply {
            duration = 2_400L
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
        ObjectAnimator.ofFloat(logo, "scaleY", 0.92f, 1.0f, 0.92f).apply {
            duration = 2_400L
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun startLoading() {
        lifecycleScope.launch {
            // small initial breathing-room so user sees the splash branding
            delay(700)
            val repo = PlaylistRepository(applicationContext)
            val url = PrefsManager.playlistUrl
            val result = repo.load(url) { msg, progress ->
                runOnUiThread {
                    status.text = msg
                    animateProgress(progress)
                }
            }
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { channels ->
                        PlaylistCache.channels = channels
                        delay(500)
                        startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    },
                    onFailure = { err ->
                        status.text = getString(R.string.fetch_failed, err.message ?: "Unknown")
                        // Allow user to still see the splash; HomeActivity will show empty state
                        delay(2000)
                        startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    private fun animateProgress(target: Int) {
        ObjectAnimator.ofInt(pb, "progress", pb.progress, target).apply {
            duration = 320
            start()
        }
    }
}
