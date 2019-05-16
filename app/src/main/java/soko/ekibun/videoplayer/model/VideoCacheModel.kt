package soko.ekibun.videoplayer.model

import android.content.Context
import android.preference.PreferenceManager
import com.google.gson.reflect.TypeToken
import soko.ekibun.util.JsonUtil
import soko.ekibun.videoplayer.bean.SubjectCache
import soko.ekibun.videoplayer.bean.VideoCache
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject
import kotlin.collections.HashMap

class VideoCacheModel(context: Context){
    private val sp by lazy{ PreferenceManager.getDefaultSharedPreferences(context)!! }
    private val cacheList by lazy { JsonUtil.toEntity<HashMap<String, SubjectCache>>(sp.getString(PREF_VIDEO_CACHE, JsonUtil.toJson(HashMap<String, SubjectCache>()))!!,
        object : TypeToken<HashMap<String, SubjectCache>>() {}.type)?: HashMap() }

    fun getCacheList(site: String): List<SubjectCache>{
        return cacheList.filter { it.key.startsWith(site) }.values.toList()
    }

    fun getSubjectCacheList(subject: VideoSubject): SubjectCache?{
        return cacheList[subjectKey(subject)]
    }

    fun getVideoCache(episode: VideoEpisode, subject: VideoSubject): VideoCache? {
        return getSubjectCacheList(subject)?.videoList?.firstOrNull { it.episode.id == episode.id }
    }

    fun addVideoCache(subject: VideoSubject, cache: VideoCache){
        val editor = sp.edit()
        cacheList[subjectKey(subject)] = SubjectCache(subject, (cacheList[subjectKey(subject)]?.videoList?: ArrayList()).filterNot { it.episode.id == cache.episode.id }.plus(cache))
        editor.putString(PREF_VIDEO_CACHE, JsonUtil.toJson(cacheList))
        editor.apply()
    }

    fun removeVideoCache(episode: VideoEpisode, subject: VideoSubject){
        val editor = sp.edit()
        cacheList[subjectKey(subject)] = SubjectCache(subject, (cacheList[subjectKey(subject)]?.videoList?: ArrayList()).filterNot { it.episode.id == episode.id })
        cacheList[subjectKey(subject)]?.let{
            if(it.videoList.isEmpty()) cacheList.remove(subjectKey(subject))
        }
        editor.putString(PREF_VIDEO_CACHE, JsonUtil.toJson(cacheList))
        editor.apply()
    }

    companion object {
        const val PREF_VIDEO_CACHE = "videoCache"

        fun isFinished(downloadPercentage: Float): Boolean{
            return Math.abs(downloadPercentage - 100f) < 0.001f
        }

        fun subjectKey(subject: VideoSubject): String{
            return "${subject.site}_${subject.id}"
        }
    }
}