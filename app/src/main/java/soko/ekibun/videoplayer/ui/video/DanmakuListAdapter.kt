package soko.ekibun.videoplayer.ui.video

import android.content.res.ColorStateList
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_provider.view.*
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.model.LineInfoModel
import soko.ekibun.videoplayer.model.VideoProvider

class DanmakuListAdapter(data: MutableList<DanmakuInfo>? = null) :
        BaseQuickAdapter<DanmakuListAdapter.DanmakuInfo, BaseViewHolder>(R.layout.item_provider, data) {

    override fun convert(helper: BaseViewHolder, item: DanmakuInfo) {
        helper.itemView.item_title.text = item.line.id
        val provider = App.from(helper.itemView.context).videoProvider.getProvider(item.line.site)?:return
        helper.itemView.item_switch.visibility = View.GONE
        helper.itemView.item_site.backgroundTintList = ColorStateList.valueOf((0xff000000 + provider.color).toInt())
        helper.itemView.item_site.text = provider.title
        helper.itemView.item_id.text = if(item.info.isNotEmpty()) item.info else " ${item.danmakus.size} 条弹幕"
    }

    data class DanmakuInfo(
        val line: LineInfoModel.LineInfo,
        var danmakus: HashSet<VideoProvider.DanmakuInfo> = HashSet(),
        var info: String = "",
        var videoInfo: VideoProvider.VideoInfo? = null,
        var key: String? = null
    )
}