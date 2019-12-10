package soko.ekibun.videoplayer.model

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import soko.ekibun.util.JsonUtil
import soko.ekibun.videoplayer.JsEngine
import soko.ekibun.videoplayer.ui.dialog.ProviderAdapter

class MangaProvider(context: Context): ProviderAdapter.LineProvider<MangaProvider.ProviderInfo> {
    class ProviderInfo(
        site: String,
        color: Int,
        title: String,
        search: String = "",
        @ProviderAdapter.Code("获取剧集列表", 1) val getEpisode: String = "", // (line: LineInfo) -> List<MangaEpisode>
        @ProviderAdapter.Code("获取图片列表", 2) val getManga: String = "",   // (episode: MangaEpisode) -> List<ImageInfo>
        @ProviderAdapter.Code("获取图片", 3) val getImage: String = ""        // (image: ImageInfo) -> ImageRequest
    ): ProviderAdapter.ProviderInfo(site, color, title, search) {
        fun getEpisode(scriptKey: String, jsEngine: JsEngine, line: LineInfoModel.LineInfo): JsEngine.ScriptTask<List<MangaEpisode>> {
            return JsEngine.ScriptTask(jsEngine,"var line = ${JsonUtil.toJson(line)};\n$getEpisode", scriptKey){
                JsonUtil.toEntity<List<MangaEpisode>>(it)!!
            }
        }
        fun getManga(scriptKey: String, jsEngine: JsEngine, episode: MangaEpisode): JsEngine.ScriptTask<List<ImageInfo>> {
            return JsEngine.ScriptTask(jsEngine,"var episode = ${JsonUtil.toJson(episode)};\n$getManga", scriptKey){
                JsonUtil.toEntity<List<ImageInfo>>(it)!!
            }
        }
        fun getImage(scriptKey: String, jsEngine: JsEngine, image: ImageInfo): JsEngine.ScriptTask<ImageRequest> {
            return JsEngine.ScriptTask(jsEngine,"var image = ${JsonUtil.toJson(image)};\n${
            if(getImage.isNotEmpty()) getImage else "return image.url;"}", scriptKey){
                JsonUtil.toEntity<ImageRequest>(it)!!
            }
        }
    }

    data class MangaEpisode(
        val site: String,
        val id: String,
        val sort: String,
        val title: String,
        val url: String
    )

    data class ImageInfo(
        val site: String?,
        val id: String?,
        val url: String
    )

    data class ImageRequest(
        val url: String,
        val header: HashMap<String, String> = HashMap()
    )

    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }
    override val providerList by lazy { JsonUtil.toEntity<HashMap<String, ProviderInfo>>(sp.getString(
        PREF_MANGA_PROVIDER, JsonUtil.toJson(HashMap<String, ProviderInfo>()))!!)?: HashMap() }

    override fun getProvider(site: String): ProviderInfo?{
        return providerList[site]
    }

    override fun addProvider(provider: ProviderInfo){
        val editor = sp.edit()
        providerList[provider.site] = provider
        editor.putString(PREF_MANGA_PROVIDER, JsonUtil.toJson(providerList))
        editor.apply()
    }

    override fun removeProvider(site: String){
        val editor = sp.edit()
        providerList.remove(site)
        editor.putString(PREF_MANGA_PROVIDER, JsonUtil.toJson(providerList))
        editor.apply()
    }

    companion object{
        const val PREF_MANGA_PROVIDER="mangaProvider"
    }
}