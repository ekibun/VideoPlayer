package soko.ekibun.videoplayer.ui.splash

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import soko.ekibun.videoplayer.ui.setting.SettingsActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SettingsActivity.startActivity(this)
        finish()
    }
}
