package soko.ekibun.videoplayer.ui.video.line

import android.content.Context
import com.zhy.adapter.abslistview.CommonAdapter
import com.zhy.adapter.abslistview.ViewHolder
import soko.ekibun.videoplayer.model.VideoProvider

class ProviderAdapter(context: Context?, data: List<VideoProvider.ProviderInfo>?) : CommonAdapter<VideoProvider.ProviderInfo>(context, android.R.layout.simple_spinner_dropdown_item, data) {

    override fun convert(viewHolder: ViewHolder, item: VideoProvider.ProviderInfo, position: Int) {
        viewHolder.setText(android.R.id.text1, item.title)
    }
}