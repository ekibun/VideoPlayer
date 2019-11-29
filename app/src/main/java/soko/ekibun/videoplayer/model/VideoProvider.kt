package soko.ekibun.videoplayer.model

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import soko.ekibun.util.JsonUtil
import soko.ekibun.videoplayer.JsEngine
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.ui.dialog.ProviderAdapter

class VideoProvider(context: Context): ProviderAdapter.LineProvider<VideoProvider.ProviderInfo> {
    class ProviderInfo(
        site: String,
        color: Int,
        title: String,
        search: String = "",
        val getVideoInfo: String = "",  // (line: LineInfo, episode: VideoEpisode) -> VideoInfo
        val getVideo: String = "",      // (video: VideoInfo) -> VideoRequest
        val getDanmakuKey: String = "", // (video: VideoInfo) -> String
        val getDanmaku: String = ""     // (video: VideoInfo, key: String, pos: Int) -> List<DanmakuInfo>
    ): ProviderAdapter.ProviderInfo(site, color, title, search) {
        val hasDanmaku get() = getDanmaku.isNotEmpty()

        fun getVideoInfo(scriptKey: String, jsEngine: JsEngine, line: LineInfoModel.LineInfo, episode: VideoEpisode): JsEngine.ScriptTask<VideoInfo>{
            return JsEngine.ScriptTask(jsEngine,"var line = ${JsonUtil.toJson(line)};var episode = ${JsonUtil.toJson(episode)};\n$getVideoInfo", scriptKey){
                JsonUtil.toEntity<VideoInfo>(it)!!
            }
        }

        fun getVideo(scriptKey: String, jsEngine: JsEngine, video: VideoInfo): JsEngine.ScriptTask<VideoRequest>{
            return JsEngine.ScriptTask(jsEngine,"var video = ${JsonUtil.toJson(video)};\n${if(getVideo.isNotEmpty()) getVideo else "return webview.load(video.url);"}", scriptKey){
                JsonUtil.toEntity<VideoRequest>(it)!!
            }
        }

        fun getDanmakuKey(scriptKey: String, jsEngine: JsEngine, video: VideoInfo): JsEngine.ScriptTask<String>{
            return JsEngine.ScriptTask(jsEngine,"var video = ${JsonUtil.toJson(video)};\n${if(getDanmakuKey.isNotEmpty()) getDanmakuKey else "return \"\";"}", scriptKey){ it }
        }

        fun getDanmaku(scriptKey: String, jsEngine: JsEngine, video: VideoInfo, key: String, pos: Int): JsEngine.ScriptTask<List<DanmakuInfo>>{
            return JsEngine.ScriptTask(jsEngine,"var video = ${JsonUtil.toJson(video)};var key = $key;var pos = $pos;\n$getDanmaku", scriptKey){
                JsonUtil.toEntity<List<DanmakuInfo>>(it)?:ArrayList()
            }
        }
    }

    data class VideoInfo(
        val site: String,
        val id:String,
        val url: String
    )

    data class VideoRequest(
        val url: String,
        val header: HashMap<String, String> = HashMap(),
        val overrideExtension: String? = null
    )

    data class DanmakuInfo(
        val time: Float,
        val type: Int,
        val textSize: Float,
        val color: Int,
        val content: String,
        val timeStamp: Long = 0L
    )

    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }
    override val providerList by lazy { JsonUtil.toEntity<HashMap<String, ProviderInfo>>(sp.getString(
        PREF_VIDEO_PROVIDER, JsonUtil.toJson(HashMap<String, ProviderInfo>()))!!)?: HashMap() }

    override fun getProvider(site: String): ProviderInfo?{
        return providerList[site]
    }

    override fun addProvider(provider: ProviderInfo){
        val editor = sp.edit()
        providerList[provider.site] = provider
        editor.putString(PREF_VIDEO_PROVIDER, JsonUtil.toJson(providerList))
        editor.apply()
    }

    override fun removeProvider(site: String){
        val editor = sp.edit()
        providerList.remove(site)
        editor.putString(PREF_VIDEO_PROVIDER, JsonUtil.toJson(providerList))
        editor.apply()
    }

    companion object{
        const val PREF_VIDEO_PROVIDER="videoProvider"
    }
}