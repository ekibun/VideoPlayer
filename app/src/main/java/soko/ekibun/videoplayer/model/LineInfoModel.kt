package soko.ekibun.videoplayer.model

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import soko.ekibun.util.JsonUtil
import soko.ekibun.videoplayer.bean.VideoSubject

class LineInfoModel(context: Context){
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

    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }
    private fun prefKey(subject: VideoSubject): String{
        return PREF_LINE_INFO_LIST + VideoCacheModel.subjectKey(subject)
    }

    fun saveInfos(subject: VideoSubject, infos: LineInfoList) {
        val editor = sp.edit()
        val key = prefKey(subject)
        if(infos.providers.size == 0){
            editor.remove(key)
        }else{
            editor.putString(key, JsonUtil.toJson(infos))
        }
        editor.apply()
    }

    fun getInfos(subject: VideoSubject): LineInfoList? {
        return JsonUtil.toEntity<LineInfoList>(sp.getString(prefKey(subject), JsonUtil.toJson(LineInfoList()))!!)
    }

    companion object{
        const val PREF_LINE_INFO_LIST="lineInfoList"
    }
}