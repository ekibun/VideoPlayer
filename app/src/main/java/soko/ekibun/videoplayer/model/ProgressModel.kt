package soko.ekibun.videoplayer.model

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import soko.ekibun.util.JsonUtil
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject

class ProgressModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }
    private fun prefKey(subject: VideoSubject): String{
        return "${PREF_PROGRESS_INFO}_${subject.site}_${subject.id}"
    }

    fun saveProgress(subject: VideoSubject, info: Info) {
        val editor = sp.edit()
        val key = prefKey(subject)
        editor.putString(key, JsonUtil.toJson(info))
        editor.apply()
    }

    fun getProgress(subject: VideoSubject): Info? {
        return JsonUtil.toEntity(sp.getString(prefKey(subject), "")!!, Info::class.java)
    }

    data class Info(
        val episode: VideoEpisode,
        val progress: Int)

    companion object {
        const val PREF_PROGRESS_INFO="progressInfo"
    }
}