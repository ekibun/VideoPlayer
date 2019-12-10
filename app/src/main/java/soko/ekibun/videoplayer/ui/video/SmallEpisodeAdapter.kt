package soko.ekibun.videoplayer.ui.video

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_episode_small.view.*
import soko.ekibun.util.ResourceUtil
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.model.VideoCacheModel

class SmallEpisodeAdapter(val context: Context, data: MutableList<VideoEpisode>? = null) :
        BaseQuickAdapter<VideoEpisode, BaseViewHolder>
        (R.layout.item_episode_small, data) {
    private val videoCacheModel by lazy{ App.from(context).videoCacheModel }

    override fun convert(helper: BaseViewHolder, item: VideoEpisode) {
        helper.setText(R.id.item_title, item.parseSort())
        helper.setText(R.id.item_desc, item.name)
        val color = ResourceUtil.resolveColorAttr(helper.itemView.context,
                when {
                    !item.progress.isNullOrEmpty() -> R.attr.colorPrimary
                    else -> android.R.attr.textColorSecondary
                })
        helper.itemView.item_title.setTextColor(color)
        helper.itemView.item_desc.setTextColor(color)
        helper.itemView.item_badge.visibility = if(item.progress != null) View.VISIBLE else View.INVISIBLE
        helper.itemView.item_badge.backgroundTintList = ColorStateList.valueOf(color)
        helper.itemView.item_badge.text = item.progress
        helper.itemView.item_container.backgroundTintList = ColorStateList.valueOf(color)
        helper.addOnClickListener(R.id.item_container)
        helper.addOnLongClickListener(R.id.item_container)

        // TODO Compat Manga
        (context as? VideoActivity)?.subjectPresenter?.let {
            val videoCache = videoCacheModel.getVideoCache(item, it.subject)
            updateDownload(helper.itemView, videoCache?.percentDownloaded?: Float.NaN, videoCache?.bytesDownloaded?:0L, videoCache != null)
        }
    }
    fun updateDownload(view: View, percent: Float, bytes: Long, hasCache: Boolean, download: Boolean = false){
        view.item_icon.visibility = if(hasCache) View.VISIBLE else View.INVISIBLE
        view.item_icon.setImageResource(when {
            VideoCacheModel.isFinished(percent) -> R.drawable.ic_episode_download_ok
            download -> R.drawable.ic_episode_download
            else -> R.drawable.ic_episode_download_pause
        })
    }
}