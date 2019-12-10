package soko.ekibun.videoplayer.ui.video

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.entity.SectionEntity
import com.oushangfeng.pinnedsectionitemdecoration.PinnedHeaderItemDecoration
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.subject_detail.*
import kotlinx.android.synthetic.main.subject_episode.*
import kotlinx.android.synthetic.main.subject_infolist.*
import soko.ekibun.util.GlideUtil
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.model.SubjectProvider.Companion.EXTRA_SUBJECT

class SubjectView(val context: VideoActivity) {
    val episodeAdapter = SmallEpisodeAdapter(context)
    val episodeDetailAdapter = EpisodeAdapter(context)
    val lineAdapter = LineAdapter()
    val seasonAdapter = SeasonAdapter()
    val seasonLayoutManager = LinearLayoutManager(context)

    init {
        context.episode_list.adapter = episodeAdapter
        context.episode_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        context.episode_list.isNestedScrollingEnabled = false

        context.episode_detail_list.adapter = episodeDetailAdapter
        context.episode_detail_list.addItemDecoration(PinnedHeaderItemDecoration.Builder(episodeDetailAdapter.sectionHeader).create())
        context.episode_detail_list.layoutManager = LinearLayoutManager(context)

        context.line_list.adapter = lineAdapter
        context.line_list.layoutManager = LinearLayoutManager(context)

        context.season_list.adapter = seasonAdapter
        seasonLayoutManager.orientation = RecyclerView.HORIZONTAL
        context.season_list.layoutManager = seasonLayoutManager
        context.season_list.isNestedScrollingEnabled = false

        context.item_close.setOnClickListener {
            showEpisodeDetail(false)
        }
        context.episode_detail.setOnClickListener{
            showEpisodeDetail(true)
        }
    }

    private val weekList = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
    @SuppressLint("SetTextI18n")
    fun updateSubject(subject: VideoSubject){
        if(context.isDestroyed) return
        context.runOnUiThread {
            context.item_detail.setOnClickListener {
                try{
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("ekibun://playersubject/${subject.site}/${subject.id}"))
                    intent.putExtra(EXTRA_SUBJECT, subject)
                    context.startActivity(intent)
                }catch (e: Exception){ e.printStackTrace() }
            }

            context.title = subject.name
            //subject detail
            context.item_info.text = subject.type
            context.item_subject_title.text = subject.name
            context.item_air_time.text = "开播时间：${subject.air_date}"
            context.item_air_week.text = {
                var ret = "更新时间："
                subject.air_weekday.toString().forEach {
                    ret += weekList[it.toString().toInt()] + " "
                }
                ret
            }()
            context.item_score.text = subject.rating.toString()
            context.item_score_count.text = "${subject.rating_count}人"
            GlideUtil.with(context.item_cover)
                ?.load(subject.image)
                ?.apply(RequestOptions.errorOf(R.drawable.ic_404).placeholder(context.item_cover.drawable))
                ?.into(context.item_cover)
            GlideUtil.with(context.item_cover_blur)
                ?.load(subject.image)
                ?.apply(RequestOptions.bitmapTransform(BlurTransformation(25, 8)).placeholder(context.item_cover_blur.drawable))
                ?.into(context.item_cover_blur)
            updateEpisode(subject.eps, subject, false)
            //collection
            context.item_collect_info.text = if(subject.collect.isNullOrEmpty()) "收藏" else subject.collect
            context.item_collect_image.setImageDrawable(context.resources.getDrawable(
                if(subject.collect.isNullOrEmpty()) R.drawable.ic_heart_outline else R.drawable.ic_heart, context.theme))
        }
    }

    var subjectEpisode: List<VideoEpisode> = ArrayList()
    private var scrolled = false
    @SuppressLint("SetTextI18n")
    fun updateEpisode(episodes: List<VideoEpisode>?, subject: VideoSubject, merge: Boolean){
        if(context.isDestroyed) return
        context.runOnUiThread {
            val cacheEpisode = (App.from(context).videoCacheModel.getSubjectCacheList(subject)?.videoList?.map { it.episode }?:ArrayList()).sortedBy { it.sort }.sortedBy { it.cat }
            episodes?.forEach { ep ->
                ep.merge(subjectEpisode.firstOrNull { it.id == ep.id } ?: return@forEach)
            }
            val newSubjectEpisode = if(merge) (episodes?:listOf()).plus(subjectEpisode).distinctBy { it.id } else episodes?: subjectEpisode
            if(subjectEpisode.size != newSubjectEpisode.size) scrolled = false
            subjectEpisode = newSubjectEpisode
            val eps = subjectEpisode.filter { (it.status?:"") in listOf("Air") }
            context.episode_detail.text = (if(cacheEpisode.isNotEmpty()) "已缓存 ${cacheEpisode.size} 话" else "") +
                    (if(cacheEpisode.isNotEmpty() && subjectEpisode.isNotEmpty()) " / " else "")+
                    (if(subjectEpisode.isNotEmpty()) (if(eps.size == subjectEpisode.size) "全 ${eps.size} 话" else eps.lastOrNull()?.let{ "更新到${it.parseSort()}" }?:"尚未更新") else "")
            val maps = LinkedHashMap<String, List<VideoEpisode>>()
            subjectEpisode.plus(cacheEpisode).distinctBy { it.id }.forEach {
                val key = it.cat?:""
                maps[key] = (maps[key]?:ArrayList()).plus(it)
            }
            episodeAdapter.setNewData(null)
            episodeDetailAdapter.setNewData(null)
            maps.forEach {
                episodeDetailAdapter.addData(object: SectionEntity<VideoEpisode>(true, it.key){})
                it.value.forEach {
                    if((it.status?:"") in listOf("Air"))
                        episodeAdapter.addData(it)
                    episodeDetailAdapter.addData(object: SectionEntity<VideoEpisode>(it){})
                }
            }
            if(!scrolled){
                scrolled = true

                var lastView = 0
                episodeAdapter.data.forEachIndexed { index, episode ->
                    if(episode.progress != null)
                        lastView = index
                }
                val layoutManager = (context.episode_list.layoutManager as LinearLayoutManager)
                layoutManager.scrollToPositionWithOffset(lastView, 0)
                layoutManager.stackFromEnd = false
            }
        }
    }

    fun showEpisodeDetail(show: Boolean){
        context.episode_detail_list_header.visibility = if(show) View.VISIBLE else View.INVISIBLE
        context.episode_detail_list_header.animation = AnimationUtils.loadAnimation(context, if(show) R.anim.move_in_bottom else R.anim.move_out_bottom)
        context.episode_detail_list.visibility = if(show) View.VISIBLE else View.INVISIBLE
        context.episode_detail_list.animation = AnimationUtils.loadAnimation(context, if(show) R.anim.move_in_bottom else R.anim.move_out_bottom)
    }
}