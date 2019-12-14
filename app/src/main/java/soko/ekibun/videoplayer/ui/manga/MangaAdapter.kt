package soko.ekibun.videoplayer.ui.manga

import android.content.Context
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_image.view.*
import soko.ekibun.util.GlideUtil
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.model.MangaProvider

class MangaAdapter(context: Context, data: MutableList<MangaProvider.ImageInfo>? = null) :
    BaseQuickAdapter<MangaProvider.ImageInfo, BaseViewHolder>(R.layout.item_image, data) {
    val requests = HashMap<MangaProvider.ImageInfo, MangaProvider.ImageRequest>()
    val jsEngine by lazy { App.from(context).jsEngine }
    val mangaProvider by lazy { App.from(context).mangaProvider }

    override fun convert(helper: BaseViewHolder, item: MangaProvider.ImageInfo) {
        helper.itemView.image_sort.text = item.index.toString()
        helper.itemView.tag = item
        helper.itemView.item_loading.setOnClickListener {
            if(helper.itemView.tag == item) loadData(helper, item)
        }
        loadData(helper, item)
    }

    private fun loadData(helper: BaseViewHolder, item: MangaProvider.ImageInfo){
        helper.itemView.item_loading.isClickable = false
        helper.itemView.item_image.visibility = View.INVISIBLE
        helper.itemView.item_loading.visibility = View.VISIBLE
        helper.itemView.loading_progress.visibility = View.VISIBLE
        helper.itemView.loading_text.visibility = View.GONE
        helper.itemView.loading_progress.isIndeterminate = true
        val imageRequest = requests[item]?: if(item.site.isNullOrEmpty()) MangaProvider.ImageRequest(item.url) else null
        if(imageRequest != null){
            setImage(helper, item, imageRequest)
        }else{
            mangaProvider.getProvider(item.site?:"")?.getImage("${item.site}_${item.id}", jsEngine, item)?.enqueue({
                requests[item] = it
                setImage(helper, item, it)
            }, {
                if(helper.itemView.tag == item){
                    showError(helper, "接口错误")
                }
            })?: {
                showError(helper, "接口不存在")
            }()
        }
    }

    private fun showError(helper: BaseViewHolder, message: String){
        helper.itemView.item_loading.isClickable = true
        helper.itemView.loading_progress.visibility = View.GONE
        helper.itemView.loading_text.visibility = View.VISIBLE
        helper.itemView.loading_text.text = message
    }

    private fun setImage(helper: BaseViewHolder, item: MangaProvider.ImageInfo, imageRequest: MangaProvider.ImageRequest){
        helper.itemView.loading_progress.progress = 0
        GlideUtil.loadWithProgress(imageRequest.url, helper.itemView.item_image,  {
            helper.itemView.loading_progress.isIndeterminate = false
            if(helper.itemView.tag == item) helper.itemView.loading_progress.progress = (it * 100).toInt()
        }, { type, drawable ->
            helper.itemView.item_image.setImageDrawable(drawable)
            if(helper.itemView.tag == item) {
                when(type){
                    GlideUtil.TYPE_ERROR -> {
                        showError(helper, "加载出错")
                    }
                    GlideUtil.TYPE_RESOURCE -> {
                        helper.itemView.item_image.visibility = View.VISIBLE
                        helper.itemView.item_loading.visibility = View.GONE
                    }
                }
            }
        })
    }
}