package soko.ekibun.videoplayer.ui.video

import android.annotation.SuppressLint
import android.view.View
import android.view.animation.AnimationUtils
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.error_frame.*
import kotlinx.android.synthetic.main.video_player.*
import soko.ekibun.videoplayer.model.ProgressModel
import soko.ekibun.videoplayer.ui.view.controller.Controller
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.bean.VideoEpisode

class VideoView(val context: VideoActivity) {
    @SuppressLint("SetTextI18n")
    fun updateProgress(info: ProgressModel.Info?) {
        if(info == null){
            context.item_progress_info.text = "尚未观看"
            context.item_progress_play.text = " 从头开始"
            context.item_progress.setOnClickListener {
                context.videoPresenter.playEpisode(context.subjectPresenter.subjectView.episodeDetailAdapter.data.firstOrNull{ it.t != null}?.t?: VideoEpisode(sort = 1f))
            }
        }else {
            context.item_progress_info.text = "上次看到 ${info.episode.parseSort()} ${Controller.stringForTime(info.progress)}"
            context.item_progress_play.text = " 继续观看"
            context.item_progress.setOnClickListener {
                context.videoPresenter.playEpisode(info.episode)
                context.videoPresenter.startAt = info.progress * 10L
            }
        }
    }

    var loadVideoInfo: Boolean? = null
        set(v) {
            field = v
            parseLogcat()
        }
    var loadVideo: Boolean? = null
        set(v) {
            field = v
            parseLogcat()
        }
    var loadDanmaku: Boolean? = null
        set(v) {
            field = v
            parseLogcat()
        }
    var exception: Exception? = null
        set(v) {
            field = v
            parseLogcat()
        }
    @SuppressLint("SetTextI18n")
    private fun parseLogcat(){
        context.runOnUiThread{
            if(loadVideoInfo == false || loadVideo == false || exception != null)
                context.videoPresenter.controller.updateLoading(false)
            context.item_logcat.text = "获取视频信息…" + if(loadVideoInfo == null) "" else (
                    if(loadVideoInfo != true) "【失败】" else ("【完成】" +
                            "\n解析视频地址…${if(loadVideo == null) "" else if(loadVideo == true) "【完成】" else "【失败】"}" +
                            "\n全舰弹幕装填…${if(loadDanmaku == null) "" else if(loadDanmaku == true) "【完成】" else "【失败】"}" +
                            if(loadVideo == true) "\n开始视频缓冲…" else "")) +
                    if(exception != null) "\n$exception" else ""
        }
    }

    fun showVideoError(error: String, retry: String, callback: ()->Unit){
        context.runOnUiThread {
            context.videoPresenter.doPlayPause(false)
            context.videoPresenter.controller.doShowHide(false)
            context.item_error_hint.text = error
            context.item_retry_button.text = retry
            context.error_frame.visibility = View.VISIBLE
            context.item_retry_button.setOnClickListener {
                context.error_frame.visibility = View.INVISIBLE
                callback()
            }
        }
    }

    fun initPlayer(episode: VideoEpisode){
        loadVideoInfo = null
        loadVideo = null
        loadDanmaku = null
        exception = null
        context.runOnUiThread {
            context.error_frame.visibility = View.INVISIBLE
            context.toolbar_layout.isTitleEnabled = false
            context.video_surface_container.visibility = View.VISIBLE
            context.video_surface.visibility = View.VISIBLE
            context.controller_frame.visibility = View.VISIBLE
            context.item_logcat.visibility = View.VISIBLE
            context.toolbar.subtitle = episode.parseSort() + " - " + episode.name
        }
    }

    fun showDanmakuSetting(show: Boolean){
        if(context.danmaku_setting_panel.visibility == if(show) View.VISIBLE else View.INVISIBLE) return
        context.danmaku_setting_panel.visibility = if(show) View.VISIBLE else View.INVISIBLE
        context.danmaku_setting_panel.animation = AnimationUtils.loadAnimation(context, if(show) R.anim.move_in_right else R.anim.move_out_right)
    }
}