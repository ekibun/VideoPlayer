package soko.ekibun.videoplayer.ui.video

import android.content.res.ColorStateList
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_season.view.*
import soko.ekibun.videoplayer.R
import soko.ekibun.util.ResourceUtil
import soko.ekibun.videoplayer.bean.VideoSubject

class SeasonAdapter(data: MutableList<VideoSubject>? = null) :
        BaseQuickAdapter<VideoSubject, BaseViewHolder>
        (R.layout.item_season, data) {
    var currentId = ""

    override fun convert(helper: BaseViewHolder, item: VideoSubject) {
        helper.setText(R.id.item_desc, item.name)
        helper.itemView.item_desc.setBackgroundResource(
                when(data.indexOf(item)){
                    0-> R.drawable.bangumi_detail_ic_season_first
                    data.size-1-> R.drawable.bangumi_detail_ic_season_last
                    else-> R.drawable.bangumi_detail_ic_season_middle
                }
        )
        val color = ResourceUtil.resolveColorAttr(helper.itemView.context,
                if (currentId == item.id) R.attr.colorPrimary
                    else android.R.attr.textColorSecondary)
        helper.itemView.item_desc.setTextColor(color)
        helper.itemView.item_desc.backgroundTintList = ColorStateList.valueOf(color)
    }
}