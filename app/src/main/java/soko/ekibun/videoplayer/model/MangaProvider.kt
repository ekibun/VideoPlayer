package soko.ekibun.videoplayer.model

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import soko.ekibun.util.JsonUtil
import soko.ekibun.videoplayer.JsEngine
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.ui.dialog.ProviderAdapter

class MangaProvider(context: Context): ProviderAdapter.LineProvider<MangaProvider.ProviderInfo> {
    class ProviderInfo(
        site: String,
        color: Int,
        title: String,
        search: String = "",
        val getManga: String = ""       // (line: LineInfo, episode: VideoEpisode) -> List<String>
    ): ProviderAdapter.ProviderInfo(site, color, title, search) {
        fun getManga(scriptKey: String, jsEngine: JsEngine, line: LineInfoModel.LineInfo, episode: VideoEpisode): JsEngine.ScriptTask<List<String>> {
            return JsEngine.ScriptTask(jsEngine,"var line = ${JsonUtil.toJson(line)};var episode = ${JsonUtil.toJson(episode)};\n$getManga", scriptKey){
                JsonUtil.toEntity<List<String>>(it)?:ArrayList()
            }
        }
    }

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