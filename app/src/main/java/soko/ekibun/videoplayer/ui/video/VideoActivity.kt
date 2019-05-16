package soko.ekibun.videoplayer.ui.video

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
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
import soko.ekibun.util.JsonUtil
import soko.ekibun.util.StorageUtil
import soko.ekibun.util.SwipeBackActivity
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.model.SubjectProvider
import soko.ekibun.videoplayer.model.VideoProvider
import soko.ekibun.videoplayer.service.DownloadService
import soko.ekibun.videoplayer.ui.provider.ProviderActivity

class VideoActivity : SwipeBackActivity() {
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

        registerReceiver(receiver, IntentFilter(ACTION_MEDIA_CONTROL + subjectPresenter.subject.id))
        registerReceiver(downloadReceiver, IntentFilter(DownloadService.getBroadcastAction(subjectPresenter.subject)))

    }

    var pauseOnStop = false
    override fun onStart() {
        super.onStart()
        if(videoPresenter.videoModel.player.duration >0 && pauseOnStop)
            videoPresenter.doPlayPause(true)
        pauseOnStop = false

        subjectPresenter.refreshSubject()
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
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        onMultiWindowModeChanged((Build.VERSION.SDK_INT >=24 && isInMultiWindowMode), newConfig)
        super.onConfigurationChanged(newConfig)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration?) {
        systemUIPresenter.onWindowModeChanged(isInMultiWindowMode, (Build.VERSION.SDK_INT >=24 && isInPictureInPictureMode), newConfig)
        if(video_surface_container.visibility == View.VISIBLE) videoPresenter.controller.doShowHide(false)
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
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

    override fun processBack(){
        if(systemUIPresenter.isLandscape || videoPresenter.videoModel.player.playWhenReady || episode_detail_list.visibility == View.VISIBLE) return
        super.processBack()
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
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_video, menu)
        return true
    }

    private fun checkStorage(): Boolean{
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_STORAGE_CODE)
            return false
        }
        return true
    }

    private var loadFileCallback:((String?)-> Unit)? = null
    fun loadFile(callback:(String?)-> Unit){
        loadFileCallback = callback
        if (!checkStorage()) return
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, REQUEST_FILE_CODE)
    }

    private var loadProviderCallback:((VideoProvider.ProviderInfo?)-> Unit)? = null
    fun loadProvider(info: VideoProvider.ProviderInfo?, callback:(VideoProvider.ProviderInfo?)-> Unit){
        loadProviderCallback = callback
        val intent = Intent(this, ProviderActivity::class.java)
        info?.let { intent.putExtra(ProviderActivity.EXTRA_PROVIDER_INFO, JsonUtil.toJson(it)) }
        startActivityForResult(intent, REQUEST_PROVIDER)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && loadFileCallback != null) {
                loadFile(loadFileCallback!!)
            } else {
                loadFileCallback?.invoke(null)
                loadFileCallback = null
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        subjectPresenter.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_FILE_CODE && resultCode == RESULT_OK) {//文件
            val uri = data?.data?: return
            val path = StorageUtil.getRealPathFromUri(this, uri)
            loadFileCallback?.invoke(path)
        }

        if (requestCode == REQUEST_PROVIDER && resultCode == RESULT_OK) {//Provider
            loadProviderCallback?.invoke(JsonUtil.toEntity(data?.getStringExtra(ProviderActivity.EXTRA_PROVIDER_INFO)?:"", VideoProvider.ProviderInfo::class.java))
        }
    }

    companion object {
        const val ACTION_MEDIA_CONTROL = "soko.ekibun.videoplayer.action.mediaControl"
        const val EXTRA_CONTROL_TYPE = "extraControlType"
        const val CONTROL_TYPE_PAUSE = 1
        const val CONTROL_TYPE_PLAY = 2
        const val CONTROL_TYPE_NEXT = 3
        const val CONTROL_TYPE_PREV = 4

        private const val REQUEST_STORAGE_CODE = 1
        private const val REQUEST_FILE_CODE = 2
        private const val REQUEST_PROVIDER = 3

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
