package soko.ekibun.videoplayer

import android.app.Application
import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.database.DatabaseProvider
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.offline.DefaultDownloadIndex
import com.google.android.exoplayer2.offline.Download
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import soko.ekibun.util.StorageUtil
import soko.ekibun.videoplayer.model.LineInfoModel
import soko.ekibun.videoplayer.model.ProgressModel
import soko.ekibun.videoplayer.model.VideoCacheModel
import soko.ekibun.videoplayer.model.VideoProvider
import java.util.concurrent.Executors


/**
 * Placeholder application to facilitate overriding Application methods for debugging and testing.
 */
class App:Application() {
    val handler = Handler{ true }
    val jsEngine by lazy { JsEngine(this) }
    val videoProvider by lazy { VideoProvider(this) }
    val lineInfoModel by lazy { LineInfoModel(this) }
    val progressModel by lazy { ProgressModel(this) }
    val videoCacheModel by lazy { VideoCacheModel(this) }
    val databaseProvider by lazy { ExoDatabaseProvider(this) }
    val downloadCache by lazy { SimpleCache(StorageUtil.getDiskCacheDir(this, "video"), NoOpCacheEvictor(), databaseProvider) }

    companion object {
        val cachedThreadPool = Executors.newCachedThreadPool()

        fun from(context: Context): App {
            return context.applicationContext as App
        }
    }
}