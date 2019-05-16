package soko.ekibun.videoplayer.ui.video

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.preference.PreferenceManager
import android.view.View
import android.view.WindowManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.danmaku_setting.*
import kotlinx.android.synthetic.main.video_player.*
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.model.ProgressModel
import soko.ekibun.videoplayer.model.VideoModel
import soko.ekibun.videoplayer.model.VideoProvider
import soko.ekibun.videoplayer.ui.view.VideoController
import java.util.*

class VideoPresenter(val context: VideoActivity) {
    val subject get() = context.subjectPresenter.subject
    val subjectView get() = context.subjectPresenter.subjectView
    val lineInfoModel by lazy { App.from(context).lineInfoModel }
    val progressModel by lazy { App.from(context).progressModel }

    val videoView = VideoView(context)

    val danmakuPresenter: DanmakuPresenter by lazy{
        DanmakuPresenter(context.danmaku_flame, context) {
            videoView.exception = it?:videoView.exception
            videoView.loadDanmaku = it == null
        }
    }

    val controller: VideoController by lazy {
        VideoController(context.controller_frame, context, object: VideoController.OnActionListener{
            override fun onPlayPause() {
                doPlayPause(!videoModel.player.playWhenReady)
            }

            override fun onFullscreen() { context.requestedOrientation = if(context.systemUIPresenter.isLandscape)
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE }

            override fun onNext() {
                nextEpisode()?.let{playEpisode(it)}
            }

            override fun onPrev() {
                prevEpisode()?.let{playEpisode(it)}
            }

            override fun onDanmaku() {
                if(danmakuPresenter.view.isShown)
                    danmakuPresenter.view.hide() else danmakuPresenter.view.show()
                controller.updateDanmaku(danmakuPresenter.view.isShown)
            }

            override fun seekTo(pos: Long) {
                videoModel.player.seekTo(pos)
                controller.updateProgress(videoModel.player.currentPosition)
            }

            override fun onDanmakuSetting() {
                videoView.showDanmakuSetting(true)
                controller.doShowHide(false)
            }

            override fun onTitle() {
                doPlayPause(false)
                context.app_bar.setExpanded(false)
            }

            override fun onShowHide(show: Boolean) {
                context.runOnUiThread {
                    if (show) {
                        updatePauseResume()
                        updateProgress()
                        controller.updatePrevNext(prevEpisode() != null, nextEpisode() != null)
                        context.item_mask.visibility = View.VISIBLE
                        context.toolbar.visibility = View.VISIBLE
                        if (context.systemUIPresenter.isLandscape)
                            context.systemUIPresenter.setSystemUiVisibility(SystemUIPresenter.Visibility.FULLSCREEN_IMMERSIVE)
                    } else {
                        context.item_mask.visibility = View.INVISIBLE
                        if (!context.systemUIPresenter.isLandscape || offset == 0)
                            context.toolbar.visibility = View.INVISIBLE
                        if (context.systemUIPresenter.isLandscape && offset == 0)
                            context.systemUIPresenter.setSystemUiVisibility(SystemUIPresenter.Visibility.FULLSCREEN)
                    }
                }
            }
        }){ context.systemUIPresenter.isLandscape }
    }

    var videoWidth = 0
    var videoHeight = 0
    fun resizeVideoSurface(){
        if(videoWidth * videoHeight == 0) return
        when(PreferenceManager.getDefaultSharedPreferences(context).getInt(DanmakuPresenter.VIDEO_FRAME, DanmakuPresenter.VIDEO_FRAME_AUTO)){
            DanmakuPresenter.VIDEO_FRAME_AUTO -> {
                context.video_surface.scaleX = Math.min(context.video_surface.measuredWidth.toFloat(), (context.video_surface.measuredHeight * videoWidth / videoHeight).toFloat()) / context.video_surface.measuredWidth
                context.video_surface.scaleY = Math.min(context.video_surface.measuredHeight.toFloat(), (context.video_surface.measuredWidth * videoHeight / videoWidth).toFloat()) / context.video_surface.measuredHeight
            }
            DanmakuPresenter.VIDEO_FRAME_STRENTCH -> {
                context.video_surface.scaleX = 1f
                context.video_surface.scaleY = 1f
            }
            DanmakuPresenter.VIDEO_FRAME_FILL -> {
                context.video_surface.scaleX = Math.max(context.video_surface.measuredWidth.toFloat(), (context.video_surface.measuredHeight * videoWidth / videoHeight).toFloat()) / context.video_surface.measuredWidth
                context.video_surface.scaleY = Math.max(context.video_surface.measuredHeight.toFloat(), (context.video_surface.measuredWidth * videoHeight / videoWidth).toFloat()) / context.video_surface.measuredHeight
            }
            DanmakuPresenter.VIDEO_FRAME_16_9 -> {
                context.video_surface.scaleX = Math.min(context.video_surface.measuredWidth.toFloat(), (context.video_surface.measuredHeight * 16 / 9).toFloat()) / context.video_surface.measuredWidth
                context.video_surface.scaleY = Math.min(context.video_surface.measuredHeight.toFloat(), (context.video_surface.measuredWidth * 9 / 16).toFloat()) / context.video_surface.measuredHeight
            }
            DanmakuPresenter.VIDEO_FRAME_4_3 -> {
                context.video_surface.scaleX = Math.min(context.video_surface.measuredWidth.toFloat(), (context.video_surface.measuredHeight * 4 / 3).toFloat()) / context.video_surface.measuredWidth
                context.video_surface.scaleY = Math.min(context.video_surface.measuredHeight.toFloat(), (context.video_surface.measuredWidth * 3 / 4).toFloat()) / context.video_surface.measuredHeight
            }
        }
    }
    var endFlag = false
    val videoModel: VideoModel by lazy{
        VideoModel(context, object : VideoModel.Listener {
            override fun onReady(playWhenReady: Boolean) {
                if (!controller.ctrVisibility) {
                    controller.ctrVisibility = true
                    context.item_logcat.visibility = View.INVISIBLE
                    controller.doShowHide(false)
                }
                if (playWhenReady) {
                    doPlayPause(true)
                    startAt?.let{
                        videoModel.player.seekTo(it)
                        startAt = null
                    }
                }
                if (!controller.isShow) {
                    context.item_mask.visibility = View.INVISIBLE
                    context.toolbar.visibility = View.INVISIBLE
                }
                controller.updateLoading(false)
                endFlag = true
            }

            override fun onBuffering() {
                danmakuPresenter.view.pause()
                controller.updateLoading(true)
            }

            override fun onEnded() {
                doPlayPause(false)
                if(endFlag) {
                    endFlag = false
                    nextEpisode()?.let { playEpisode(it) }
                }
            }

            override fun onVideoSizeChange(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                videoWidth = (width * pixelWidthHeightRatio).toInt()
                videoHeight = height
                resizeVideoSurface()
            }

            override fun onError(error: ExoPlaybackException) {
                videoView.exception = error.sourceException
                Snackbar.make(context.root_layout, videoView.exception.toString(), Snackbar.LENGTH_SHORT).show()
            }
        })
    }


    var offset = 0
    fun init(){
        videoView.updateProgress(progressModel.getProgress(subject))

        context.app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener{ _, verticalOffset ->
            val visible = if(verticalOffset != 0 || controller.isShow || context.video_surface_container.visibility != View.VISIBLE) View.VISIBLE else View.INVISIBLE
            offset = verticalOffset
            if(context.systemUIPresenter.isLandscape && context.video_surface_container.visibility == View.VISIBLE && visible != context.toolbar.visibility)
                context.systemUIPresenter.setSystemUiVisibility(if(visible == View.VISIBLE) SystemUIPresenter.Visibility.FULLSCREEN_IMMERSIVE else SystemUIPresenter.Visibility.FULLSCREEN)
            if(context.systemUIPresenter.isLandscape && context.video_surface_container.visibility == View.VISIBLE){
                if(offset == 0){
                    context.window.statusBarColor = Color.TRANSPARENT
                    context.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                }else{
                    context.window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                }
            }
            context.toolbar.visibility = visible
        })
        context.hide_danmaku_panel.setOnClickListener {
            videoView.showDanmakuSetting(false)
        }
    }

    fun playEpisode(ep: VideoEpisode){
        val infos = lineInfoModel.getInfos(subject)
        infos?.getDefaultProvider()?.let{
            context.videoPresenter.prevEpisode = {
                val position = subjectView.episodeDetailAdapter.data.indexOfFirst { it.t?.id == ep.id || (it.t?.cat == ep.cat && it.t?.sort == ep.sort) }
                val episode = subjectView.episodeDetailAdapter.data.getOrNull(position-1)?.t
                if((episode?.status?:"") !in listOf("Air")) null else episode
            }
            context.videoPresenter.nextEpisode = {
                val position = subjectView.episodeDetailAdapter.data.indexOfFirst { it.t?.id == ep.id || (it.t?.cat == ep.cat && it.t?.sort == ep.sort) }
                val episode = subjectView.episodeDetailAdapter.data.getOrNull(position+1)?.t
                if((episode?.status?:"") !in listOf("Air")) null else episode
            }
            context.videoPresenter.startAt = null
            context.runOnUiThread { context.videoPresenter.play(ep, subject, it, infos.providers) }
        }?: Snackbar.make(context.root_layout, "请先添加播放源", Snackbar.LENGTH_SHORT).show()
    }
    var nextEpisode: ()->VideoEpisode? = { null }
    var prevEpisode: ()->VideoEpisode? = { null }
    var updatePlayProgress: (Int)->Unit = {}
    var startAt: Long? = null
    private fun play(episode: VideoEpisode, subject: VideoSubject, info: VideoProvider.LineInfo, infos: List<VideoProvider.LineInfo>){
        updatePlayProgress = {
            progressModel.saveProgress(subject, ProgressModel.Info(episode, it))
        }
        videoModel.player.playWhenReady = false
        context.systemUIPresenter.appbarCollapsible(false)
        videoView.initPlayer(episode)
        controller.updatePrevNext(prevEpisode() != null, nextEpisode() != null)
        controller.updateLoading(true)
        controller.ctrVisibility = false
        controller.doShowHide(true)
        controller.setTitle(context.toolbar.subtitle.toString())
        playLoopTask?.cancel()
        danmakuPresenter.view.pause()
        context.item_logcat.setOnClickListener {}

        videoModel.getVideo("play", episode, subject, info, {videoInfo, error->
            videoView.exception = error?:videoView.exception
            videoView.loadVideoInfo = videoInfo != null
            if(videoInfo != null) context.runOnUiThread {
                context.item_logcat.setOnClickListener {
                    try{ context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(videoInfo.url)), videoInfo.url)) }
                    catch (e: Exception){ e.printStackTrace() }}
                danmakuPresenter.loadDanmaku(infos.filter { it.loadDanmaku && App.from(context).videoProvider.getProvider(it.site)?.hasDanmaku == true }, episode) }
        },{request, streamKeys, error ->
            videoView.exception = error?:videoView.exception
            if(context.isDestroyed) return@getVideo
            videoView.loadVideo = request != null
            if(request != null) videoModel.play(request, context.video_surface, streamKeys)
        })
    }

    private var playLoopTask: TimerTask? = null
    fun doPlayPause(play: Boolean){
        videoModel.player.playWhenReady = play
        updatePauseResume()
        playLoopTask?.cancel()
        if(play){
            playLoopTask = object: TimerTask(){ override fun run() {
                context.runOnUiThread {
                    updateProgress()
                    updatePlayProgress((videoModel.player.currentPosition/ 10).toInt())
                    danmakuPresenter.add(videoModel.player.currentPosition)
                    if(danmakuPresenter.view.isShown && !danmakuPresenter.view.isPaused){
                        danmakuPresenter.view.start(videoModel.player.currentPosition)
                    }
                }
            } }
            controller.timer.schedule(playLoopTask, 0, 1000)
            context.video_surface.keepScreenOn = true
            if(videoModel.player.playbackState == Player.STATE_READY)
                danmakuPresenter.view.resume()
        }else{
            context.video_surface.keepScreenOn = false
            danmakuPresenter.view.pause()
        }
        context.systemUIPresenter.appbarCollapsible(!play)
    }

    private fun updateProgress(){
        context.runOnUiThread {
            controller.duration = videoModel.player.duration.toInt() /10
            controller.buffedPosition = videoModel.player.bufferedPosition.toInt() /10
            controller.updateProgress(videoModel.player.currentPosition)
        }
    }

    private fun updatePauseResume() {
        context.runOnUiThread {
            controller.updatePauseResume(videoModel.player.playWhenReady)
            context.setPictureInPictureParams(!videoModel.player.playWhenReady)
        }
    }
}