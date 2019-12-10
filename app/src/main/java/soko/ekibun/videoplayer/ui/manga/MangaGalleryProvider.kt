package soko.ekibun.videoplayer.ui.manga

import android.content.Context
import android.util.Log
import com.hippo.glgallery.GalleryProvider
import com.hippo.image.Image
import soko.ekibun.util.GlideUtil
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.model.MangaProvider

class MangaGalleryProvider(private val context: Context): GalleryProvider() {
    val data = ArrayList<MangaProvider.ImageInfo>()
    private val requests = HashMap<MangaProvider.ImageInfo, MangaProvider.ImageRequest>()
    private val jsEngine by lazy { App.from(context).jsEngine }
    private val mangaProvider by lazy { App.from(context).mangaProvider }

    override fun getError(): String {
        return "网络错误"
    }

    override fun onCancelRequest(index: Int) {
        // TODO
    }

    override fun onForceRequest(index: Int) {
        val item = data[index]
        val imageRequest = requests[item]?: if(item.site.isNullOrEmpty()) MangaProvider.ImageRequest(
            item.url
        ) else null
        if(imageRequest != null){
            getImage(index, imageRequest)
        }else{
            mangaProvider.getProvider(item.site?:"")?.getImage("${item.site}_${item.id}", jsEngine, item)?.enqueue({
                requests[item] = it
                getImage(index, it)
            }, {
                notifyPageFailed(index, "接口错误")
            })?: {
                notifyPageFailed(index, "接口不存在")
            }()
        }
    }

    private fun getImage(index: Int, imageRequest: MangaProvider.ImageRequest){
        Log.v("image", imageRequest.toString())
        notifyPagePercent(index, 0f)
        GlideUtil.loadWithProgress(imageRequest.url, context, {
            notifyPagePercent(index, it)
        }, { type, bitmap ->
            when(type){
                GlideUtil.TYPE_ERROR -> {
                    notifyPageFailed(index, "加载出错")
                }
                GlideUtil.TYPE_RESOURCE -> {
                    notifyPageSucceed(index, Image.create(bitmap))
                }
            }
        })
    }

    override fun size(): Int = if(data.size > 0) data.size else -1

    override fun onRequest(index: Int) {
        onForceRequest(index)
    }

}