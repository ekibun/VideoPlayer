package soko.ekibun.videoplayer.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.IDownloadCacheProvider
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.callback.IListSubjectCacheCallback
import soko.ekibun.videoplayer.callback.ISubjectCacheCallback
import soko.ekibun.videoplayer.callback.IVideoCacheCallback

class DownloadCacheProvider : Service() {
    val videoCacheModel by lazy { App.from(this).videoCacheModel }

    private var mDownloadCacheProvider = object: IDownloadCacheProvider.Stub() {
        override fun getEpisodeCache(subject: VideoSubject, episode: VideoEpisode, callback: IVideoCacheCallback) {
            try{
                callback.onFinish(videoCacheModel.getVideoCache(episode, subject))
            }catch(e: Exception){
                callback.onReject(e.toString())
            }
        }

        override fun getCacheList(site: String, callback: IListSubjectCacheCallback) {
            try{
                callback.onFinish(videoCacheModel.getCacheList(site))
            }catch(e: Exception){
                callback.onReject(e.toString())
            }
        }

        override fun getSubjectCache(subject: VideoSubject, callback: ISubjectCacheCallback) {
            try{
                callback.onFinish(videoCacheModel.getSubjectCacheList(subject))
            }catch(e: Exception){
                callback.onReject(e.toString())
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return mDownloadCacheProvider
    }
}
