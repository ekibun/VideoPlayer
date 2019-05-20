package soko.ekibun.videoplayer.ui.video.line

import android.annotation.SuppressLint
import android.app.Activity
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

object SearchDialog {
    @SuppressLint("InflateParams")
    fun showDialog(context: Activity, subject: VideoSubject, callback:(VideoProvider.LineInfo, Boolean)->Unit){
        val view =context.layoutInflater.inflate(R.layout.dialog_search_line, null)
        val dialog = BottomSheetDialog(context)
        dialog.setContentView(view)
        dialog.window?.decorView?.findViewById<FrameLayout>(R.id.design_bottom_sheet)?.let{
            val layoutParams = it.layoutParams
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            it.layoutParams = layoutParams
            it.setBackgroundResource(0)
        }

        val popList = ListPopupWindow(context)
        popList.anchorView = view.item_video_api
        val providers = ArrayList(App.from(context).videoProvider.providerList.values.filter { it.search.isNotEmpty() })
        providers.add(0, VideoProvider.ProviderInfo("", 0, "所有接口"))
        popList.setAdapter(ProviderAdapter(context, providers))
        popList.isModal = true

        view.item_search_key.setText(subject.name)
        view.list_search.layoutManager = LinearLayoutManager(context)
        val adapter = SearchLineAdapter()
        adapter.setOnItemClickListener { _, _, position ->
            callback(adapter.data[position], false)
        }
        adapter.setOnItemLongClickListener { _, _, position ->
            callback(adapter.data[position], true)
            dialog.dismiss()
            true
        }
        view.list_search.adapter = adapter
        val searchCall = ArrayList<Pair<VideoProvider.ProviderInfo, JsEngine.ScriptTask<List<VideoProvider.LineInfo>>>>()

        var pos = 0
        view.item_video_api.text = providers[pos]?.title?:""
        view.item_video_api.setOnClickListener {
            popList.show()
            popList.listView?.setOnItemClickListener { _, _, position, _ ->
                popList.dismiss()
                pos = position
                view.item_video_api.text = providers[pos]?.title?:""
            }
        }
        view.item_search.setOnClickListener {
            adapter.setNewData(null)
            val key = view.item_search_key.text.toString()
            searchCall.forEach { it.second.cancel(true) }
            searchCall.clear()

            val jsEngine = App.from(context).jsEngine
            searchCall.addAll(if(pos == 0){
                providers.filter { it.search.isNotEmpty() }.map {
                    Pair(it, it.search("search_${it.site}", jsEngine, key))
                }
            }else listOf(providers[pos].let{ Pair(it, it.search("search_${it.site}", jsEngine, key)) }))

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
}