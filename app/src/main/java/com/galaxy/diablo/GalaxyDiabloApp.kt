package com.galaxy.diablo

import android.app.Application
import com.galaxy.diablo.data.PrefsManager

class GalaxyDiabloApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        PrefsManager.init(this)
    }

    companion object {
        @JvmStatic
        lateinit var instance: GalaxyDiabloApp
            private set
    }
}
