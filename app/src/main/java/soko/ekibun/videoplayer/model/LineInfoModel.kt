package soko.ekibun.videoplayer.model

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import soko.ekibun.util.JsonUtil
import soko.ekibun.videoplayer.bean.VideoSubject

class LineInfoModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }
    private fun prefKey(subject: VideoSubject): String{
        return PREF_LINE_INFO_LIST + VideoCacheModel.subjectKey(subject)
    }

    fun saveInfos(subject: VideoSubject, infos: VideoProvider.LineInfoList) {
        val editor = sp.edit()
        val key = prefKey(subject)
        if(infos.providers.size == 0){
            editor.remove(key)
        }else{
            editor.putString(key, JsonUtil.toJson(infos))
        }
        editor.apply()
    }

    fun getInfos(subject: VideoSubject): VideoProvider.LineInfoList? {
        return JsonUtil.toEntity(sp.getString(prefKey(subject), JsonUtil.toJson(VideoProvider.LineInfoList()))!!, VideoProvider.LineInfoList::class.java)
    }

    companion object{
        const val PREF_LINE_INFO_LIST="lineInfoList"
    }
}