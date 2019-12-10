package soko.ekibun.videoplayer.ui.video

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.subject_episode.*
import soko.ekibun.util.AppUtil
import soko.ekibun.util.NetworkUtil
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.model.SubjectProvider
import soko.ekibun.videoplayer.model.VideoProvider
import soko.ekibun.videoplayer.service.DownloadService
import soko.ekibun.videoplayer.ui.dialog.ProviderAdapter
import soko.ekibun.videoplayer.ui.setting.SettingsActivity

class VideoActivity : ProviderAdapter.LineProviderActivity<VideoProvider.ProviderInfo>() {
    override val classT = VideoProvider.ProviderInfo::class.java
    override val fileType: String = "video/*"
    override val lineProvider by lazy { App.from(this).videoProvider }

    val subjectPresenter by lazy { SubjectPresenter(this) }
    val systemUIPresenter by lazy { SystemUIPresenter(this) }
    val videoPresenter by lazy { VideoPresenter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        subjectPresenter.init()
        systemUIPresenter.init()
        videoPresenter.init()

        val episodePaddingBottom = episode_detail_list.paddingBottom
        val listPaddingBottom = subject_detail.paddingBottom
        root_layout.setOnApplyWindowInsetsListener { _, insets ->
            episode_detail_list.setPadding(episode_detail_list.paddingLeft, episode_detail_list.paddingTop, episode_detail_list.paddingRight, episodePaddingBottom + insets.systemWindowInsetBottom)
            subject_detail.setPadding(subject_detail.paddingLeft, subject_detail.paddingTop, subject_detail.paddingRight, listPaddingBottom + insets.systemWindowInsetBottom)
            insets
        }

        registerReceiver(receiver, IntentFilter(ACTION_MEDIA_CONTROL + subjectPresenter.subject.id))
        registerReceiver(downloadReceiver, IntentFilter(DownloadService.getBroadcastAction(subjectPresenter.subject)))
        registerReceiver(networkReceiver, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"))
    }

    private var pauseOnStop = false
    override fun onStart() {
        super.onStart()
        if(videoPresenter.videoModel.player.duration >0 && pauseOnStop)
            videoPresenter.doPlayPause(true)
        pauseOnStop = false
    }

    override fun onStop() {
        super.onStop()
        if(videoPresenter.videoModel.player.playWhenReady)
            pauseOnStop = true
        videoPresenter.doPlayPause(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        subjectPresenter.destroy()
        unregisterReceiver(receiver)
        unregisterReceiver(downloadReceiver)
        unregisterReceiver(networkReceiver)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        systemUIPresenter.onWindowModeChanged(newConfig)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration?) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        systemUIPresenter.onWindowModeChanged(newConfig)
    }

    private val downloadReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            try{
                val episode = intent.getParcelableExtra<VideoEpisode>(DownloadService.EXTRA_EPISODE)!!
                val percent = intent.getFloatExtra(DownloadService.EXTRA_PERCENT, Float.NaN)
                val bytes = intent.getLongExtra(DownloadService.EXTRA_BYTES, 0L)

                val index = subjectPresenter.subjectView.episodeDetailAdapter.data.indexOfFirst { it.t?.id == episode.id }
                subjectPresenter.subjectView.episodeDetailAdapter.getViewByPosition(episode_detail_list, index, R.id.item_layout)?.let{
                    subjectPresenter.subjectView.episodeDetailAdapter.updateDownload(it, percent, bytes, intent.getBooleanExtra(DownloadService.EXTRA_CANCEL, true), !intent.hasExtra(DownloadService.EXTRA_CANCEL))
                }

                val epIndex =subjectPresenter.subjectView.episodeAdapter.data.indexOfFirst { it.id == episode.id }
                subjectPresenter.subjectView.episodeAdapter.getViewByPosition(episode_list, epIndex, R.id.item_layout)?.let{
                    subjectPresenter.subjectView.episodeAdapter.updateDownload(it, percent, bytes, intent.getBooleanExtra(DownloadService.EXTRA_CANCEL, true), !intent.hasExtra(DownloadService.EXTRA_CANCEL))
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    private val networkReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(!NetworkUtil.isWifiConnected(context) && videoPresenter.videoModel.player.playWhenReady){
                videoPresenter.doPlayPause(true)
                Toast.makeText(context, "正在使用非wifi网络", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val receiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.getIntExtra(EXTRA_CONTROL_TYPE,0)){
                CONTROL_TYPE_PAUSE ->{
                    videoPresenter.doPlayPause(false)
                }
                CONTROL_TYPE_PLAY ->{
                    videoPresenter.doPlayPause(true)
                }
                CONTROL_TYPE_NEXT ->
                    videoPresenter.nextEpisode()?.let{videoPresenter.playEpisode(it)}
                CONTROL_TYPE_PREV ->
                    videoPresenter.prevEpisode()?.let{videoPresenter.playEpisode(it)}
            }
        }
    }

    public override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if(systemUIPresenter.isLandscape && videoPresenter.videoModel.player.playWhenReady && Build.VERSION.SDK_INT >= 24) {
            @Suppress("DEPRECATION") enterPictureInPictureMode()
            setPictureInPictureParams(false)
        }
    }

    fun setPictureInPictureParams(playPause: Boolean){
        if(Build.VERSION.SDK_INT >= 26) {
            val actionPrev = RemoteAction(
                Icon.createWithResource(this, R.drawable.ic_prev), "上一集", "上一集",
                PendingIntent.getBroadcast(this, CONTROL_TYPE_PREV, Intent(ACTION_MEDIA_CONTROL + subjectPresenter.subject.id).putExtra(EXTRA_CONTROL_TYPE,
                    CONTROL_TYPE_PREV), PendingIntent.FLAG_UPDATE_CURRENT))
            actionPrev.isEnabled = videoPresenter.prevEpisode() != null
            val actionNext = RemoteAction(
                Icon.createWithResource(this, R.drawable.ic_next),  "下一集",  "下一集",
                PendingIntent.getBroadcast(this, CONTROL_TYPE_NEXT, Intent(ACTION_MEDIA_CONTROL + subjectPresenter.subject.id).putExtra(EXTRA_CONTROL_TYPE,
                    CONTROL_TYPE_NEXT), PendingIntent.FLAG_UPDATE_CURRENT))
            actionNext.isEnabled = videoPresenter.nextEpisode() != null
            try{
                setPictureInPictureParams(
                    PictureInPictureParams.Builder().setActions(listOf(
                        actionPrev,
                        RemoteAction(
                            Icon.createWithResource(this, if (playPause) R.drawable.ic_play else R.drawable.ic_pause), if (playPause)"播放" else "暂停", if (playPause)"播放" else "暂停",
                            PendingIntent.getBroadcast(this, CONTROL_TYPE_PLAY, Intent(ACTION_MEDIA_CONTROL + subjectPresenter.subject.id).putExtra(EXTRA_CONTROL_TYPE,
                                if (playPause) CONTROL_TYPE_PLAY else CONTROL_TYPE_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT)),
                        actionNext
                    )).build())
            }catch(e: Exception){ }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            when {
                systemUIPresenter.isLandscape -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                Build.VERSION.SDK_INT > 23 && isInMultiWindowMode -> Toast.makeText(this, "请先退出多窗口模式", Toast.LENGTH_SHORT).show()
                episode_detail_list.visibility == View.VISIBLE -> subjectPresenter.subjectView.showEpisodeDetail(false)
                else -> finish()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> when {
                systemUIPresenter.isLandscape -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> finish()
            }
            R.id.action_share -> AppUtil.shareString(this, "${subjectPresenter.subject.name}\n${subjectPresenter.subject.url}")
            R.id.action_refresh -> subjectPresenter.refreshSubject()
            R.id.action_settings -> SettingsActivity.startActivity(this)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_video, menu)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        subjectPresenter.onActivityResult(requestCode, resultCode, data)
        subjectPresenter.refreshSubject()
    }

    companion object {
        const val ACTION_MEDIA_CONTROL = "soko.ekibun.videoplayer.action.mediaControl"
        const val EXTRA_CONTROL_TYPE = "extraControlType"
        const val CONTROL_TYPE_PAUSE = 1
        const val CONTROL_TYPE_PLAY = 2
        const val CONTROL_TYPE_NEXT = 3
        const val CONTROL_TYPE_PREV = 4

        fun startActivity(context: Context, subject: VideoSubject, newTask:Boolean = false) {
            context.startActivity(parseIntent(context, subject, newTask))
        }

        fun parseIntent(context: Context, subject: VideoSubject, newTask:Boolean = true): Intent {
            val intent = Intent(context, VideoActivity::class.java)
            if(newTask) intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            intent.putExtra(SubjectProvider.EXTRA_SUBJECT, subject)
            return intent
        }
    }
}
