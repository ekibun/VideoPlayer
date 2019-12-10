package soko.ekibun.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDelegate

object ThemeUtil {

    /**
     * 应用导航栏主题
     */
    fun updateNavigationTheme(activity: Activity) {
        updateNavigationTheme(activity.window, activity, false)
    }

    /**
     * 应用导航栏主题
     */
    fun updateNavigationTheme(window: Window, context: Context, dialog: Boolean = true) {
        val light = AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES
        if(light) window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        val color =  ResourceUtil.resolveColorAttr(context, android.R.attr.colorBackground)
        window.navigationBarColor = Color.argb( 200, Color.red(color), Color.green(color), Color.blue(color))
    }
}