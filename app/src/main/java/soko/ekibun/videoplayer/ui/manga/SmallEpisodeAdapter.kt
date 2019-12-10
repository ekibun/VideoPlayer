package soko.ekibun.videoplayer.ui.manga

import android.content.Context
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_episode_small.view.*
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.model.MangaProvider

class SmallEpisodeAdapter(val context: Context, data: MutableList<MangaProvider.MangaEpisode>? = null) :
        BaseQuickAdapter<MangaProvider.MangaEpisode, BaseViewHolder>
        (R.layout.item_episode_small, data) {
    override fun convert(helper: BaseViewHolder, item: MangaProvider.MangaEpisode) {
        helper.setText(R.id.item_title, if(item.sort.isEmpty()) item.id else item.sort)
        helper.setText(R.id.item_desc, item.title)
        helper.itemView.item_badge.visibility = View.INVISIBLE
        helper.addOnClickListener(R.id.item_container)
        helper.addOnLongClickListener(R.id.item_container)
    }
}