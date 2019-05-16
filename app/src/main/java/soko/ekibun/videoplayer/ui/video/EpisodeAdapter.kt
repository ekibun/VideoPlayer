package soko.ekibun.videoplayer.ui.video

import androidx.recyclerview.widget.RecyclerView
import android.text.format.Formatter
import android.view.View
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.SectionEntity
import com.google.android.exoplayer2.offline.DownloadHelper
import com.oushangfeng.pinnedsectionitemdecoration.utils.FullSpanUtil
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.item_episode.view.*
import soko.ekibun.util.ResourceUtil
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.model.VideoCacheModel
import soko.ekibun.videoplayer.service.DownloadService
import java.io.IOException

class EpisodeAdapter(val context: VideoActivity, data: MutableList<SectionEntity<VideoEpisode>>? = null) :
        BaseSectionQuickAdapter<SectionEntity<VideoEpisode>, BaseViewHolder>
        (R.layout.item_episode, R.layout.header_episode, data) {
    private val videoCacheModel by lazy{ App.from(context).videoCacheModel }

    override fun convertHead(helper: BaseViewHolder, item: SectionEntity<VideoEpisode>) {
        //helper.getView<TextView>(R.id.item_header).visibility = if(data.indexOf(item) == 0) View.GONE else View.VISIBLE
        helper.setText(R.id.item_header, item.header)
    }

    override fun convert(helper: BaseViewHolder, item: SectionEntity<VideoEpisode>) {
        val index = data.indexOfFirst{ it == item }
        helper.setText(R.id.item_title, item.t.parseSort())
        helper.setText(R.id.item_desc, item.t.name)
        val color = ResourceUtil.resolveColorAttr(helper.itemView.context,
                when {
                    !item.t.progress.isNullOrEmpty() -> R.attr.colorPrimary
                    else -> android.R.attr.textColorSecondary
                })
        val alpha = if((item.t.status?:"") in listOf("Air"))1f else 0.6f
        helper.itemView.item_title.setTextColor(color)
        helper.itemView.item_title.alpha = alpha
        helper.itemView.item_desc.setTextColor(color)
        helper.itemView.item_desc.alpha = alpha

        helper.addOnClickListener(R.id.item_download)
        helper.addOnLongClickListener(R.id.item_download)

        val videoCache = videoCacheModel.getVideoCache(item.t, context.subjectPresenter.subject)
        updateDownload(helper.itemView, videoCache?.percentDownloaded?: Float.NaN, videoCache?.bytesDownloaded?:0L, videoCache != null)
    }

    fun updateDownload(itemView: View, percent: Float, bytes: Long, hasCache: Boolean, download: Boolean = false){
        if(hasCache && !VideoCacheModel.isFinished(percent)){
            itemView.item_progress.max = 10000
            itemView.item_progress.progress = (percent * 100).toInt()
            itemView.item_download_info.text = DownloadService.parseDownloadInfo(itemView.context, percent, bytes)
            itemView.item_progress.isEnabled = download
            itemView.item_progress.visibility = View.VISIBLE
        }else{
            itemView.item_download_info.text = if(hasCache) Formatter.formatFileSize(itemView.context, bytes) else ""
            itemView.item_progress.visibility = View.INVISIBLE
        }
        itemView.item_download.setImageResource(
                if(VideoCacheModel.isFinished(percent)) R.drawable.ic_cloud_done else if(download) R.drawable.ic_pause else R.drawable.ic_download )
    }

    val sectionHeader = SECTION_HEADER_VIEW

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        FullSpanUtil.onAttachedToRecyclerView(recyclerView, this, SECTION_HEADER_VIEW)
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)
        FullSpanUtil.onViewAttachedToWindow(holder, this, SECTION_HEADER_VIEW)
    }
}