package soko.ekibun.videoplayer.ui.video.line

import android.content.res.ColorStateList
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_provider.view.*
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.model.VideoProvider

class SearchLineAdapter(data: MutableList<VideoProvider.LineInfo>? = null) :
    BaseQuickAdapter<VideoProvider.LineInfo, BaseViewHolder>(R.layout.item_provider, data) {

    override fun convert(helper: BaseViewHolder, item: VideoProvider.LineInfo) {
        helper.itemView.item_switch.visibility = View.GONE
        helper.itemView.item_title.text = if(item.title.isEmpty()) item.id else item.title
        val provider = App.from(helper.itemView.context).videoProvider.getProvider(item.site)
        helper.itemView.item_site.backgroundTintList = ColorStateList.valueOf(((provider?.color?:0) + 0xff000000).toInt())
        helper.itemView.item_site.text = provider?.title?:{ if(item.site == "") "线路" else "错误接口" }()
        helper.itemView.item_id.text = item.id
    }
}