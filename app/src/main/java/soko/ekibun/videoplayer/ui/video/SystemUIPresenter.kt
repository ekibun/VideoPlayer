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
import soko.ekibun.util.ThemeUtil

class SystemUIPresenter(private val context: VideoActivity){
    fun init(){
        setSystemUiVisibility(Visibility.IMMERSIVE)
        updateRatio()
    }

    fun updateSystemUI(){
        context.runOnUiThread {
            if(isLandscape) {
                if( context.video_surface_container.visibility != View.VISIBLE || context.item_mask.visibility == View.VISIBLE || offset != 0){
                    setSystemUiVisibility(Visibility.FULLSCREEN_IMMERSIVE)
                    context.toolbar.visibility = View.VISIBLE
                }else{
                    setSystemUiVisibility(Visibility.FULLSCREEN)
                    context.toolbar.visibility = View.INVISIBLE
                }
            } else {
                setSystemUiVisibility(Visibility.IMMERSIVE)
                context.toolbar.visibility = if(context.video_surface_container.visibility == View.VISIBLE && offset == 0) context.item_mask.visibility else View.VISIBLE
            }
            context.videoPresenter.danmakuPresenter.sizeScale = when{
                isInPictureInPictureMode -> 0.7f
                isLandscape -> 1.1f
                else-> 0.8f
            }
        }
    }

    var offset = 0
    init{
        if(Build.VERSION.SDK_INT >= 28)
            context.window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        context.app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener{ _, verticalOffset ->
            offset = verticalOffset
            updateSystemUI()
        })
    }

    private fun updateRatio(){
        val lp = context.player_container.layoutParams as ConstraintLayout.LayoutParams
        lp.dimensionRatio = if(isLandscape) ({
            val screenSize = AppUtil.getScreenSize(context)
            "h,${screenSize.height}:${screenSize.width}"
        }()) else "h,16:9"
        context.player_container.layoutParams = lp
        val lp_cf = context.controller_frame.layoutParams as ConstraintLayout.LayoutParams
        lp_cf.dimensionRatio = lp.dimensionRatio
        context.controller_frame.layoutParams = lp_cf
        context.player_container.post {
            context.videoPresenter.resizeVideoSurface()
        }
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

    fun onWindowModeChanged(newConfig: Configuration?) {
        orientation = newConfig?.orientation?:orientation
        updateSystemUI()
        updateRatio()
        if(context.video_surface_container.visibility == View.VISIBLE) context.videoPresenter.controller.doShowHide(false)
        context.videoPresenter.videoView.showDanmakuSetting(false)
    }

    val isInPictureInPictureMode get()= Build.VERSION.SDK_INT >= 24 && context.isInPictureInPictureMode
    val isInMultiWindowMode  get()= Build.VERSION.SDK_INT >= 24 && context.isInMultiWindowMode
    val isLandscape get()= isInPictureInPictureMode || (orientation != Configuration.ORIENTATION_PORTRAIT && !isInMultiWindowMode)
    var orientation = Configuration.ORIENTATION_PORTRAIT
    private fun setSystemUiVisibility(visibility: Visibility){
        Log.v("setSystemUiVisibility", visibility.name)
        when(visibility){
            Visibility.FULLSCREEN -> {
                context.root_layout.fitsSystemWindows=true
                context.app_bar.fitsSystemWindows=true
                context.video_container.fitsSystemWindows=true
                context.toolbar_layout.fitsSystemWindows = true
                context.controller_frame_container.fitsSystemWindows=true
                context.window.statusBarColor = Color.TRANSPARENT
                context.window.navigationBarColor = Color.TRANSPARENT
                updateSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
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

                updateSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            }
            Visibility.FULLSCREEN_IMMERSIVE -> {
                context.root_layout.fitsSystemWindows=true
                context.app_bar.fitsSystemWindows=true
                context.video_container.fitsSystemWindows=true
                context.toolbar_layout.fitsSystemWindows = true
                context.controller_frame_container.fitsSystemWindows=true

                updateSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            }
        }
        ThemeUtil.updateNavigationTheme(context)
    }

    private fun updateSystemUiVisibility(visibility: Int){
        context.window.decorView.systemUiVisibility = 0
        context.window.decorView.systemUiVisibility = visibility
    }

    enum class Visibility{
        FULLSCREEN, IMMERSIVE, FULLSCREEN_IMMERSIVE
    }
}