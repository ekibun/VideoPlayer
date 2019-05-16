package soko.ekibun.videoplayer.ui.video

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.util.Log
import com.google.android.material.appbar.AppBarLayout
import android.view.View
import android.view.WindowManager
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.video_player.*
import soko.ekibun.util.AppUtil

class SystemUIPresenter(private val context: VideoActivity){
    fun init(){
        setSystemUiVisibility(Visibility.IMMERSIVE)
        updateRatio()
    }

    init{
        if(Build.VERSION.SDK_INT >= 28)
            context.window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        context.window.decorView.setOnSystemUiVisibilityChangeListener{
            if(it == 0)
                if(isLandscape) {
                    if(!showMask) setSystemUiVisibility(Visibility.FULLSCREEN)
                    else setSystemUiVisibility(Visibility.FULLSCREEN_IMMERSIVE)
                } else setSystemUiVisibility(Visibility.IMMERSIVE)
        }
    }

    fun updateRatio(){
        val lp = context.player_container.layoutParams as ConstraintLayout.LayoutParams
        lp.dimensionRatio = if(isLandscape) ({
            val screenSize = AppUtil.getScreenSize(context)
            "h,${screenSize.height}:${screenSize.width}"
        }()) else "h,16:9"
        context.player_container.layoutParams = lp
        val lp_cf = context.controller_frame.layoutParams as ConstraintLayout.LayoutParams
        lp_cf.dimensionRatio = lp.dimensionRatio
        context.controller_frame.layoutParams = lp_cf
        Log.v("updateRatio", lp.dimensionRatio)
        context.videoPresenter.resizeVideoSurface()
    }

    fun appbarCollapsible(enable:Boolean){
        //content.nested_scroll.tag = true
        if(enable){
            //reactive appbar
            val params = context.toolbar_layout.layoutParams as AppBarLayout.LayoutParams
            params.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
            context.toolbar_layout.layoutParams = params
        }else{
            //expand appbar
            context.app_bar.setExpanded(true)
            (context.toolbar_layout.layoutParams as AppBarLayout.LayoutParams).scrollFlags = 0
            context.toolbar_layout.isTitleEnabled = false
        }
    }

    fun onWindowModeChanged(isInMultiWindowMode: Boolean, isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        isLandscape = isInPictureInPictureMode || (newConfig?.orientation != Configuration.ORIENTATION_PORTRAIT && !isInMultiWindowMode)

        if (isLandscape) {
            if(!showMask) setSystemUiVisibility(Visibility.FULLSCREEN)
            else setSystemUiVisibility(Visibility.FULLSCREEN_IMMERSIVE)
        }else {
            setSystemUiVisibility(Visibility.IMMERSIVE)
        }
        updateRatio()
        context.videoPresenter.videoView.showDanmakuSetting(false)
        context.videoPresenter.danmakuPresenter.sizeScale = when{
            isInPictureInPictureMode -> 0.7f
            isLandscape -> 1.1f
            else-> 0.8f
        }
    }
    var isLandscape = false
    var showMask = false
    fun setSystemUiVisibility(visibility: Visibility){
        showMask = visibility == Visibility.FULLSCREEN_IMMERSIVE
        when(visibility){
            Visibility.FULLSCREEN -> {
                context.root_layout.fitsSystemWindows=true
                context.app_bar.fitsSystemWindows=true
                context.video_container.fitsSystemWindows=true
                context.toolbar_layout.fitsSystemWindows = true
                context.controller_frame_container.fitsSystemWindows=true
                context.window.statusBarColor = Color.TRANSPARENT
                //content.window.navigationBarColor = Color.TRANSPARENT
                context.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        //or View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
            Visibility.IMMERSIVE -> {
                context.root_layout.fitsSystemWindows=true
                context.app_bar.fitsSystemWindows=true
                context.video_container.fitsSystemWindows=false
                context.controller_frame_container.fitsSystemWindows=false
                context.toolbar_layout.fitsSystemWindows = true
                context.window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

                context.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
            Visibility.FULLSCREEN_IMMERSIVE -> {
                context.root_layout.fitsSystemWindows=true
                context.app_bar.fitsSystemWindows=true
                context.video_container.fitsSystemWindows=true
                context.toolbar_layout.fitsSystemWindows = true
                context.controller_frame_container.fitsSystemWindows=true

                context.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
        }
    }

    enum class Visibility{
        FULLSCREEN, IMMERSIVE, FULLSCREEN_IMMERSIVE
    }
}