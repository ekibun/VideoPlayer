package soko.ekibun.videoplayer.ui.manga

import android.content.Context
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.model.MangaProvider

class EpisodeAdapter(val context: Context, data: MutableList<MangaProvider.MangaEpisode>? = null) :
        BaseQuickAdapter<MangaProvider.MangaEpisode, BaseViewHolder>
        (R.layout.item_episode, data) {
    override fun convert(helper: BaseViewHolder, item: MangaProvider.MangaEpisode) {
        helper.setText(R.id.item_title, if(item.sort.isEmpty()) item.id else item.sort)
        helper.setText(R.id.item_desc, item.title)
    }
}