package soko.ekibun.videoplayer.ui.dialog

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_provider.view.*
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.model.LineInfoModel

class SearchLineAdapter(data: MutableList<LineInfoModel.LineInfo>? = null) :
    BaseQuickAdapter<LineInfoModel.LineInfo, BaseViewHolder>(R.layout.item_provider, data) {
    var lines: LineInfoModel.LineInfoList? = null

    @SuppressLint("SetTextI18n")
    override fun convert(helper: BaseViewHolder, item: LineInfoModel.LineInfo) {
        val exist = lines?.providers?.firstOrNull { it.site == item.site && it.id == item.id && it.offset == item.offset } != null
        helper.itemView.item_title.alpha = if(exist) 0.7f else 1f
        helper.itemView.item_site.alpha = helper.itemView.item_title.alpha
        helper.itemView.item_id.alpha = helper.itemView.item_title.alpha

        helper.itemView.item_switch.visibility = View.GONE
        helper.itemView.item_title.text = (if(exist) "[已存在] " else "") +if(item.title.isEmpty()) item.id else item.title
        val provider = (helper.itemView.context as? ProviderAdapter.LineProviderActivity<*>)?.lineProvider?.getProvider(item.site)
        helper.itemView.item_site.backgroundTintList = ColorStateList.valueOf(((provider?.color?:0) + 0xff000000).toInt())
        helper.itemView.item_site.text = provider?.title?:{ if(item.site == "") "线路" else "错误接口" }()
        helper.itemView.item_id.text = item.id
    }
}