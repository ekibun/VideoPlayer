package soko.ekibun.videoplayer.ui.video

import android.content.res.ColorStateList
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_provider.view.*
import soko.ekibun.util.ResourceUtil
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.model.VideoProvider

class LineAdapter(data: MutableList<VideoProvider.LineInfo>? = null) :
        BaseQuickAdapter<VideoProvider.LineInfo, BaseViewHolder>(R.layout.item_provider, data) {
    var selectIndex = 0
    var onSwitchChange =  {_: Int, _: Boolean->}

    override fun convert(helper: BaseViewHolder, item: VideoProvider.LineInfo) {
        val index = data.indexOfFirst{ it === item }
        val color = ResourceUtil.resolveColorAttr(helper.itemView.context,
                if (index == selectIndex) R.attr.colorPrimary else android.R.attr.textColorSecondary)
        helper.itemView.item_title.setTextColor(color)
        helper.itemView.item_title.text = if(item.title.isEmpty()) item.id else item.title
        val provider = App.from(helper.itemView.context).videoProvider.getProvider(item.site)
        helper.itemView.item_switch.setOnCheckedChangeListener { _, _ ->  }
        helper.itemView.item_switch.isEnabled = provider?.hasDanmaku == true
        helper.itemView.item_switch.isChecked = item.loadDanmaku
        helper.itemView.item_switch.tag = index
        helper.itemView.item_switch.setOnCheckedChangeListener { _, isChecked ->
            onSwitchChange(index, isChecked)
        }
        helper.itemView.item_site.backgroundTintList = ColorStateList.valueOf(((provider?.color?:0) + 0xff000000).toInt())
        helper.itemView.item_site.text = provider?.title?:{ if(item.site == "") "线路" else "错误接口" }()
        helper.itemView.item_id.text = item.id
    }
}