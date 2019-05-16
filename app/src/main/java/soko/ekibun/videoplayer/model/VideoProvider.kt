package soko.ekibun.videoplayer.model

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.google.gson.reflect.TypeToken
import soko.ekibun.util.JsonUtil
import soko.ekibun.videoplayer.JsEngine
import soko.ekibun.videoplayer.bean.VideoEpisode

class VideoProvider(context: Context) {
    data class ProviderInfo(
        val site: String,
        val color: Int,
        val title: String,
        val search: String = "",        // (key: String) -> List<LineInfo>
        val getVideoInfo: String = "",  // (line: LineInfo, episode: VideoEpisode) -> VideoInfo
        val getVideo: String = "",      // (video: VideoInfo) -> VideoRequest
        val getDanmakuKey: String = "", // (video: VideoInfo) -> String
        val getDanmaku: String = ""     // (video: VideoInfo, key: String, pos: Int) -> List<DanmakuInfo>
    ){
        val hasDanmaku get() = getDanmaku.isNotEmpty()

        fun search(scriptKey: String, jsEngine: JsEngine, key: String): JsEngine.ScriptTask<List<LineInfo>> {
            return JsEngine.ScriptTask(jsEngine,"var key = ${JsonUtil.toJson(key)};\n$search", scriptKey){
                JsonUtil.toEntity<List<LineInfo>>(it, object : TypeToken<List<LineInfo>>() {}.type)?:ArrayList()
            }
        }

        fun getVideoInfo(scriptKey: String, jsEngine: JsEngine, line: LineInfo, episode: VideoEpisode): JsEngine.ScriptTask<VideoInfo>{
            return JsEngine.ScriptTask(jsEngine,"var line = ${JsonUtil.toJson(line)};var episode = ${JsonUtil.toJson(episode)};\n$getVideoInfo", scriptKey){
                JsonUtil.toEntity(it, VideoInfo::class.java)!!
            }
        }

        fun getVideo(scriptKey: String, jsEngine: JsEngine, video: VideoInfo): JsEngine.ScriptTask<VideoRequest>{
            return JsEngine.ScriptTask(jsEngine,"var video = ${JsonUtil.toJson(video)};\n${if(getVideo.isNotEmpty()) getVideo else "return webview.load(video.url);"}", scriptKey){
                JsonUtil.toEntity(it, VideoRequest::class.java)!!
            }
        }

        fun getDanmakuKey(scriptKey: String, jsEngine: JsEngine, video: VideoInfo): JsEngine.ScriptTask<String>{
            return JsEngine.ScriptTask(jsEngine,"var video = ${JsonUtil.toJson(video)};\n${if(getDanmakuKey.isNotEmpty()) getDanmakuKey else "return \"\";"}", scriptKey){ it }
        }

        fun getDanmaku(scriptKey: String, jsEngine: JsEngine, video: VideoInfo, key: String, pos: Int): JsEngine.ScriptTask<List<DanmakuInfo>>{
            return JsEngine.ScriptTask(jsEngine,"var video = ${JsonUtil.toJson(video)};var key = $key;var pos = $pos;\n$getDanmaku", scriptKey){
                JsonUtil.toEntity<List<DanmakuInfo>>(it, object : TypeToken<List<DanmakuInfo>>() {}.type)?:ArrayList()
            }
        }
    }

    data class LineInfo(
        var site: String,
        var id: String,
        var offset: Float = 0f,
        var title: String = "",
        var loadDanmaku: Boolean = true
    )

    data class LineInfoList(
        val providers: ArrayList<LineInfo> = ArrayList(),
        var defaultProvider: Int = 0
    ){
        fun getDefaultProvider(): LineInfo?{
            return providers.getOrNull(defaultProvider)
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
    val providerList by lazy { JsonUtil.toEntity<HashMap<String, ProviderInfo>>(sp.getString(
        PREF_VIDEO_PROVIDER, JsonUtil.toJson(HashMap<String, ProviderInfo>()))!!, object : TypeToken<HashMap<String, ProviderInfo>>() {}.type)?: HashMap() }

    fun getProvider(site: String): ProviderInfo?{
        return providerList[site]
    }

    fun addProvider(provider: ProviderInfo){
        val editor = sp.edit()
        providerList[provider.site] = provider
        editor.putString(PREF_VIDEO_PROVIDER, JsonUtil.toJson(providerList))
        editor.apply()
    }

    fun removeProvider(site: String){
        val editor = sp.edit()
        providerList.remove(site)
        editor.putString(PREF_VIDEO_PROVIDER, JsonUtil.toJson(providerList))
        editor.apply()
    }

    companion object{
        const val PREF_VIDEO_PROVIDER="videoProvider"
    }
}