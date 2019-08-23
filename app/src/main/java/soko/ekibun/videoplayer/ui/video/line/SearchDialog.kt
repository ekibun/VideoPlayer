package soko.ekibun.videoplayer.ui.video.line

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ListPopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.dialog_search_line.view.*
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.JsEngine
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.model.VideoProvider
import soko.ekibun.videoplayer.ui.video.VideoActivity

object SearchDialog {
    @SuppressLint("InflateParams")
    fun showDialog(context: VideoActivity, subject: VideoSubject, callback:()->Unit){
        val lineInfoModel = App.from(context).lineInfoModel

        val view =context.layoutInflater.inflate(R.layout.dialog_search_line, null)
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(view)
        dialog.window?.decorView?.findViewById<FrameLayout>(R.id.design_bottom_sheet)?.let{
            val layoutParams = it.layoutParams
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            it.layoutParams = layoutParams
            it.setBackgroundResource(0)
        }

        view.item_search_key.setText(subject.name)
        view.list_search.layoutManager = LinearLayoutManager(context)
        val adapter = SearchLineAdapter()
        adapter.lines = lineInfoModel.getInfos(subject)
        adapter.setOnItemClickListener { _, _, position ->
            val item = adapter.data[position]
            val exist = adapter.lines?.providers?.firstOrNull { it.site == item.site && it.id == item.id && it.offset == item.offset } != null
            if(exist) {
                Snackbar.make(dialog.window?.decorView?: view, "线路已存在，长按编辑此线路", Snackbar.LENGTH_LONG).show()
                return@setOnItemClickListener
            }
            val lines = (adapter.lines?: VideoProvider.LineInfoList())
            lines.providers.add(adapter.data[position])
            lineInfoModel.saveInfos(subject, lines)
            adapter.lines = lines
            callback()
            adapter.notifyItemChanged(position)
        }
        adapter.setOnItemLongClickListener { _, _, position ->
            val item = adapter.data[position]//?.let { item -> adapter.lines?.providers?.firstOrNull { it.site == item.site && it.id == item.id }?: item }
            LineDialog.showDialog(context, subject, item, callback)
            dialog.dismiss()
            true
        }
        view.list_search.adapter = adapter
        val searchCall = ArrayList<Pair<VideoProvider.ProviderInfo, JsEngine.ScriptTask<List<VideoProvider.LineInfo>>>>()

        view.item_video_api.text = emptyProvider.title
        view.item_video_api.tag = emptyProvider
        updateProvider(view)

        view.item_search.setOnClickListener {
            adapter.setNewData(null)
            val key = view.item_search_key.text.toString()
            searchCall.forEach { it.second.cancel(true) }
            searchCall.clear()

            val jsEngine = App.from(context).jsEngine
            val provider = view.item_video_api.tag as? VideoProvider.ProviderInfo?:emptyProvider
            searchCall.addAll(if(provider == emptyProvider){
                ArrayList(App.from(context).videoProvider.providerList.values.filter { it.search.isNotEmpty() }).filter { it.search.isNotEmpty() }.map {
                    Pair(it, it.search("search_${it.site}", jsEngine, key))
                }
            }else listOf(provider.let{ Pair(it, it.search("search_${it.site}", jsEngine, key)) }))

            searchCall.forEach {
                it.second.enqueue({lines->
                    adapter.addData(lines)
                }, {e->
                    Snackbar.make(dialog.window?.decorView?: view, "${it.first.title}: ${e.message}", Snackbar.LENGTH_LONG).show()
                })
            }
        }

        dialog.show()
    }

    private fun updateProvider(view: View){
        val popList = ListPopupWindow(view.context)
        popList.anchorView = view.item_video_api
        val videoProvider = App.from(view.context).videoProvider
        val providers = ArrayList(videoProvider.providerList.values.filter { it.search.isNotEmpty() })
        providers.add(0, emptyProvider)
        popList.setAdapter(ProviderAdapter(view.context, providers))
        popList.isModal = true

        view.item_video_api.setOnClickListener {
            popList.show()
            popList.listView?.setOnItemClickListener { _, _, position, _ ->
                popList.dismiss()
                view.item_video_api.text = providers[position].title
                view.item_video_api.tag = providers[position]
            }
            popList.listView?.setOnItemLongClickListener { _, _, position, _ ->
                popList.dismiss()
                if(position == 0) return@setOnItemLongClickListener false
                //edit
                val info = providers[position]
                (view.context as VideoActivity).loadProvider(providers[position]) {
                    videoProvider.removeProvider(info.site)
                    if (it != null) videoProvider.addProvider(it)
                    updateProvider(view)
                }
                true
            }
        }
        (view.item_video_api?.tag as? VideoProvider.LineInfo)?.let{ updateInfo(view, it) }
    }
    private fun updateInfo(view: View, info: VideoProvider.LineInfo){
        val provider = App.from(view.context).videoProvider.getProvider(info.site)?:emptyProvider
        view.item_video_api.text = provider.title
        view.item_video_api.tag = provider
    }

    private val emptyProvider = VideoProvider.ProviderInfo("", 0, "所有接口")
}