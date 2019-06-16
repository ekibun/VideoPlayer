package soko.ekibun.videoplayer.model

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.offline.DownloadHelper
import com.google.android.exoplayer2.offline.StreamKey
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.util.Util
import soko.ekibun.util.NetworkUtil
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.JsEngine
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.ui.video.VideoActivity
import java.text.DecimalFormat

class VideoModel(private val context: VideoActivity, private val onAction: Listener) {

    interface Listener{
        fun onReady(playWhenReady: Boolean)
        fun onBuffering()
        fun onEnded()
        fun onVideoSizeChange(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float)
        fun onError(error: ExoPlaybackException)
    }

    val player: SimpleExoPlayer by lazy{
        val player = ExoPlayerFactory.newSimpleInstance(context)
        player.addListener(object: Player.EventListener{
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}
            override fun onSeekProcessed() {}
            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}
            override fun onPlayerError(error: ExoPlaybackException) { onAction.onError(error) }
            override fun onLoadingChanged(isLoading: Boolean) {}
            override fun onPositionDiscontinuity(reason: Int) {}
            override fun onRepeatModeChanged(repeatMode: Int) {}
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
            override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {}
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState){
                    Player.STATE_ENDED -> onAction.onEnded()
                    Player.STATE_READY -> onAction.onReady(playWhenReady)
                    Player.STATE_BUFFERING-> onAction.onBuffering()
                }
            }
        })
        player.addVideoListener(object: com.google.android.exoplayer2.video.VideoListener{
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                onAction.onVideoSizeChange(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
            }
            override fun onRenderedFirstFrame() {}
        })
        player
    }

    private var videoInfoCall: HashMap<String, JsEngine.ScriptTask<VideoProvider.VideoInfo>> = HashMap()
    private var videoCall: HashMap<String,  JsEngine.ScriptTask<VideoProvider.VideoRequest>> = HashMap()
    //private val videoCacheModel by lazy{ App.getVideoCacheModel(content)}
    fun getVideo(key: String, episode: VideoEpisode, subject: VideoSubject, info: VideoProvider.LineInfo?,
                 onGetVideoInfo: (VideoProvider.VideoInfo?, error: Exception?)->Unit,
                 onGetVideo: (VideoProvider.VideoRequest?, List<StreamKey>?, error: Exception?)->Unit,
                 onCheckNetwork: (()->Unit)->Unit) {
        //val videoCache = videoCacheModel.getCache(episode, subject)
        val videoCache = App.from(context).videoCacheModel.getVideoCache(episode, subject)
        if (videoCache != null) {
            onGetVideoInfo(VideoProvider.VideoInfo("", videoCache.video.url, videoCache.video.url), null)
            onGetVideo(videoCache.video, videoCache.streamKeys, null)
        } else {
            val provider = App.from(context).videoProvider.getProvider(info?.site?:"")
            if(info == null || provider == null){
                if(info?.site == ""){
                    val format = (Regex("""\{\{(.*)\}\}""").find(info.id)?.groupValues?: listOf("{{ep}}", "ep")).toMutableList()
                    if(format[0] == "{{ep}}") format[1] = "#.##"
                    val url = try{ info.id.replace(format[0], DecimalFormat(format[1]).format(episode.sort + info.offset)) }catch(e: Exception){ info.id }
                    onGetVideoInfo(VideoProvider.VideoInfo("", url, url), null)
                    onGetVideo(VideoProvider.VideoRequest(url), null, null)
                }else onGetVideoInfo(null, null)
                return
            }
            val loadFromNetwork:()->Unit = {
                val jsEngine = App.from(context).jsEngine
                videoInfoCall[key]?.cancel(true)
                videoCall[key]?.cancel(true)
                videoInfoCall[key] = provider.getVideoInfo(key, jsEngine, info, episode)
                videoInfoCall[key]?.enqueue({ video ->
                    onGetVideoInfo(video, null)
                    if(video.site == ""){
                        onGetVideo(VideoProvider.VideoRequest(video.url), null, null)
                        return@enqueue
                    }
                    val videoProvider = App.from(context).videoProvider.getProvider(video.site)!!
                    videoCall[key] = videoProvider.getVideo(key, jsEngine, video)
                    videoCall[key]?.enqueue({
                        onGetVideo(it, null, null)
                    }, { onGetVideo(null, null, it) })
                }, { onGetVideoInfo(null, it) })
            }
            if(!NetworkUtil.isWifiConnected(context)) onCheckNetwork(loadFromNetwork) else loadFromNetwork()
        }
    }

    fun createMediaSource(request: VideoProvider.VideoRequest, streamKeys: List<StreamKey>? = null): MediaSource {
        val uri = Uri.parse(request.url)
        val dataSourceFactory = createDataSourceFactory(context, request, streamKeys != null)
        return when(@C.ContentType Util.inferContentType(uri, request.overrideExtension)){
            C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory).setStreamKeys(streamKeys)
            C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory).setStreamKeys(streamKeys)
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory).setStreamKeys(streamKeys)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory)
            else -> ProgressiveMediaSource.Factory(dataSourceFactory)
        }.createMediaSource(uri)
    }

    fun createDownloadRequest(request: VideoProvider.VideoRequest, callback: DownloadHelper.Callback){
        val uri = Uri.parse(request.url)
        val dataSourceFactory = createDataSourceFactory(context, request, true)
        val helper = when(@C.ContentType Util.inferContentType(uri, request.overrideExtension)){
            C.TYPE_DASH -> DownloadHelper.forDash(uri, dataSourceFactory, DefaultRenderersFactory(context))
            C.TYPE_SS -> DownloadHelper.forSmoothStreaming(uri, dataSourceFactory, DefaultRenderersFactory(context))
            C.TYPE_HLS -> DownloadHelper.forHls(uri, dataSourceFactory, DefaultRenderersFactory(context))
            C.TYPE_OTHER -> DownloadHelper.forProgressive(uri)
            else -> DownloadHelper.forProgressive(uri)
        }
        helper.prepare(callback)
    }

    var reload = {}
    fun play(request: VideoProvider.VideoRequest, surface: SurfaceView, streamKeys: List<StreamKey>? = null){
        reload = {
            player.setVideoSurfaceView(surface)
            player.prepare(createMediaSource(request, streamKeys))
            player.playWhenReady = true
        }
        reload()
    }

    companion object {
        fun createDataSourceFactory(context: Context, request: VideoProvider.VideoRequest, useCache: Boolean = false): DefaultDataSourceFactory {
            val httpSourceFactory= DefaultHttpDataSourceFactory(request.header["User-Agent"]?:"exoplayer", null, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS, DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true)
            request.header.forEach{
                httpSourceFactory.defaultRequestProperties.set(it.key, it.value)
            }
            return DefaultDataSourceFactory(context, null, if(useCache) CacheDataSourceFactory(App.from(context).downloadCache, httpSourceFactory) else httpSourceFactory)
        }
    }
}