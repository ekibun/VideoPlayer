package soko.ekibun.videoplayer.ui.setting

import android.content.ComponentName
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.*
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.ui.splash.SplashActivity

class SettingsFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(saveInstanceState: Bundle?) {
        super.onCreate(saveInstanceState)
        // 加载xml资源文件
        addPreferencesFromResource(R.xml.app_settings)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when(key){
            "hide_launcher" ->{
                val pkg = activity.packageManager
                if (sharedPreferences.getBoolean(key, false)) {
                    pkg.setComponentEnabledSetting(ComponentName(activity, SplashActivity::class.java),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                } else {
                    pkg.setComponentEnabledSetting(ComponentName(activity, SplashActivity::class.java),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }
}