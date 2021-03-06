package soko.ekibun.videoplayer.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.IBinder
import android.text.format.Formatter
import android.util.Log
import com.google.android.exoplayer2.offline.*
import soko.ekibun.util.JsonUtil
import soko.ekibun.util.NotificationUtil
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.model.VideoCacheModel
import soko.ekibun.videoplayer.model.VideoModel
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.bean.VideoCache
import soko.ekibun.videoplayer.ui.video.VideoActivity

class DownloadService : Service() {
    private val manager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val videoCacheModel by lazy { App.from(this@DownloadService).videoCacheModel }
    private val taskCollection = HashMap<String, DownloadTask>()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getTaskKey(episode: VideoEpisode, subject: VideoSubject): String{
        return VideoCacheModel.subjectKey(subject) + "_${episode.id}"
    }

    private fun getGroupSummary(status: Int): String{
        val groupKey = "download"
        manager.notify(0, NotificationUtil.builder(this, downloadChannelId, "下载")
            .setSmallIcon(when(status) {
                0 -> R.drawable.offline_pin
                -1 -> R.drawable.ic_pause
                else -> android.R.drawable.stat_sys_download
            })
            .setContentTitle("")
            .setAutoCancel(true)
            .setGroupSummary(true)
            .setGroup(groupKey)
            .build())
        return groupKey
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {request ->
            val episode = request.getParcelableExtra<VideoEpisode>(EXTRA_EPISODE)?:return@let
            val subject = request.getParcelableExtra<VideoSubject>(EXTRA_SUBJECT)?:return@let
            val taskKey = getTaskKey(episode, subject)
            Log.v("server", "${request.action} $taskKey")
            when(request.action){
                ACTION_DOWNLOAD -> {
                    val task = taskCollection[taskKey]
                    if(task!= null){
                        taskCollection.remove(taskKey)
                        task.cancel(true)
                        sendBroadcast(episode, subject, task.percentDownloaded, task.bytesDownloaded, true)
                        val pIntent = PendingIntent.getActivity(this@DownloadService, taskKey.hashCode(),
                            VideoActivity.parseIntent(this@DownloadService, subject), PendingIntent.FLAG_UPDATE_CURRENT)
                        manager.notify(taskKey, 0, NotificationUtil.builder(this@DownloadService, downloadChannelId, "下载")
                            .setSmallIcon(R.drawable.ic_pause)
                            .setOngoing(false)
                            .setAutoCancel(true)
                            .setGroup(this@DownloadService.getGroupSummary(-1))
                            .setContentTitle("已暂停 ${subject.name} ${episode.parseSort()}")
                            .setContentText(parseDownloadInfo(this@DownloadService, task.percentDownloaded, task.bytesDownloaded))
                            .setContentIntent(pIntent).build())
                    }else{
                        val videoCache = JsonUtil.toEntity<VideoCache>(intent.getStringExtra(EXTRA_VIDEO_CACHE)?:"")?:return@let
                        App.from(this@DownloadService).videoCacheModel.addVideoCache(subject, videoCache)
                        val newTask = DownloadTask(createDownloader(videoCache)) {mTask ->
                            val status = taskCollection.filter { !VideoCacheModel.isFinished(it.value.percentDownloaded) }.size

                            val isFinished = VideoCacheModel.isFinished(mTask.percentDownloaded)
                            if(isFinished) taskCollection.remove(taskKey)

                            videoCache.contentLength = mTask.contentLength
                            videoCache.bytesDownloaded = mTask.bytesDownloaded
                            videoCache.percentDownloaded = mTask.percentDownloaded

                            videoCacheModel.addVideoCache(subject, videoCache)

                            sendBroadcast(episode, subject, mTask.percentDownloaded, mTask.bytesDownloaded)
                            val pIntent = PendingIntent.getActivity(this@DownloadService, taskKey.hashCode(),
                                VideoActivity.parseIntent(this@DownloadService, subject), PendingIntent.FLAG_UPDATE_CURRENT)
                            manager.notify(taskKey, 0, NotificationUtil.builder(this@DownloadService, downloadChannelId, "下载")
                                .setSmallIcon(if(isFinished) R.drawable.offline_pin else android.R.drawable.stat_sys_download)
                                .setOngoing(!isFinished)
                                .setAutoCancel(true)
                                .setGroup(this@DownloadService.getGroupSummary(status))
                                .setContentTitle((if(isFinished)"已完成 " else "") + "${subject.name} ${episode.parseSort()}")
                                .setContentText(if(isFinished)Formatter.formatFileSize(this@DownloadService, mTask.bytesDownloaded) else parseDownloadInfo(this@DownloadService, mTask.percentDownloaded, mTask.bytesDownloaded))
                                .let{ if(!isFinished) it.setProgress(10000, (mTask.percentDownloaded * 100).toInt(), mTask.bytesDownloaded == 0L)
                                    it }
                                .setContentIntent(pIntent).build())
                        }
                        taskCollection[taskKey] = newTask
                        newTask.executeOnExecutor(App.cachedThreadPool)
                    }
                }
                ACTION_REMOVE -> {
                    manager.cancel(taskKey, 0)
                    if(taskCollection.containsKey(taskKey)){
                        taskCollection[taskKey]!!.cancel(true)
                        taskCollection.remove(taskKey)
                    }
                    if(taskCollection.isEmpty())
                        manager.cancel(0)

                    val videoCache = videoCacheModel.getVideoCache(episode, subject)?:return@let
                    createDownloader(videoCache).remove()
                    videoCacheModel.removeVideoCache(episode, subject)

                    sendBroadcast(episode, subject, Float.NaN, 0, false)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createDownloader(videoCache: VideoCache): Downloader{
        val dataSourceFactory =VideoModel.createDataSourceFactory(this, videoCache.video, true)
        val downloaderFactory = DefaultDownloaderFactory(DownloaderConstructorHelper(App.from(this).downloadCache, dataSourceFactory))
        return downloaderFactory.createDownloader(DownloadRequest(videoCache.video.url, videoCache.type, Uri.parse(videoCache.video.url), videoCache.streamKeys, null, null))
    }

    private fun sendBroadcast(episode: VideoEpisode, subject: VideoSubject, percent: Float, bytes: Long, hasCache: Boolean? = null){
        val broadcastIntent = Intent(getBroadcastAction(subject))
        broadcastIntent.putExtra(EXTRA_EPISODE, episode)
        broadcastIntent.putExtra(EXTRA_PERCENT, percent)
        broadcastIntent.putExtra(EXTRA_BYTES, bytes)
        hasCache?.let{ broadcastIntent.putExtra(EXTRA_CANCEL, it) }
        sendBroadcast(broadcastIntent)
    }

    class DownloadTask(private val downloader: Downloader, val update: (DownloadTask)->Unit): AsyncTask<Unit, Unit, Unit>(){
        var contentLength = 0L
        var bytesDownloaded = 0L
        var percentDownloaded = 0f
        override fun doInBackground(vararg params: Unit?) {
            while(!Thread.currentThread().isInterrupted && ! VideoCacheModel.isFinished(percentDownloaded)){
                try {
                    var time = System.currentTimeMillis()
                    downloader.download{ contentLength, bytesDownloaded, percentDownloaded ->
                        this.contentLength = contentLength
                        this.bytesDownloaded = bytesDownloaded
                        this.percentDownloaded = percentDownloaded
                        val now = System.currentTimeMillis()
                        if(now - time > 1000){
                            time = now
                            publishProgress()
                        }
                    }
                } catch (e: InterruptedException) {
                    break
                }catch(e: Exception){
                    e.printStackTrace()
                    Thread.sleep(1000)
                }
            }
        }
        override fun onProgressUpdate(vararg values: Unit?) {
            update(this)
            super.onProgressUpdate(*values)
        }

        override fun onPostExecute(result: Unit?) {
            if(!isCancelled) update(this)
            super.onPostExecute(result)
        }
    }

    companion object {
        const val downloadChannelId = "download"

        const val EXTRA_CANCEL = "extraCancel"
        const val EXTRA_PERCENT = "extraPercent"
        const val EXTRA_BYTES = "extraBytes"

        const val EXTRA_EPISODE = "extraEpisode"
        const val EXTRA_SUBJECT = "extraSubject"
        const val EXTRA_VIDEO_CACHE = "extraVideoCache"

        const val ACTION_DOWNLOAD = "actionDownload"
        const val ACTION_REMOVE = "actionRemove"

        fun getBroadcastAction(subject: VideoSubject): String{
            return "soko.ekibun.videoplayer.download.${subject.site}_${subject.id}"
        }

        fun parseDownloadInfo(context: Context, percent: Float, bytes: Long): String{
            return "${Formatter.formatFileSize(context, bytes)}/${Formatter.formatFileSize(context, (bytes * 100 / percent).toLong())}"
        }

        fun download(context: Context, episode: VideoEpisode, subject: VideoSubject, videoCache: VideoCache){
            val intent = Intent(context, DownloadService::class.java)
            intent.action = ACTION_DOWNLOAD
            intent.putExtra(EXTRA_SUBJECT, subject)
            intent.putExtra(EXTRA_EPISODE, episode)
            intent.putExtra(EXTRA_VIDEO_CACHE, JsonUtil.toJson(videoCache))
            context.startService(intent)
        }
        fun remove(context: Context, episode: VideoEpisode, subject: VideoSubject){
            val intent = Intent(context, DownloadService::class.java)
            intent.action = ACTION_REMOVE
            intent.putExtra(EXTRA_SUBJECT, subject)
            intent.putExtra(EXTRA_EPISODE, episode)
            context.startService(intent)
        }
    }
}
