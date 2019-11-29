package soko.ekibun.videoplayer.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ListPopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.dialog_search_line.view.*
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.JsEngine
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.model.LineInfoModel
import java.lang.reflect.Type

class SearchDialog<T: ProviderAdapter.ProviderInfo>(val context: ProviderAdapter.LineProviderActivity<T>) : Dialog(context, R.style.AppTheme_Dialog) {

    companion object {
        fun showDialog(
            context: ProviderAdapter.LineProviderActivity<*>,
            subject: VideoSubject,
            callback:()->Unit,
            typeT: Type, typeListT: Type
        ){
            val dialog = SearchDialog(context)
            dialog.subject = subject
            dialog.showLineDialog = {
                if(it != null){
                    LineDialog.showDialog(context, subject, it, callback, typeT, typeListT)
                } else callback()
            }
            dialog.show()
        }
    }

    lateinit var subject: VideoSubject
    lateinit var showLineDialog: (item: LineInfoModel.LineInfo?) -> Unit

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lineInfoModel = App.from(context).lineInfoModel

        val view =context.layoutInflater.inflate(R.layout.dialog_search_line, null)
        setContentView(view)

        view.item_outside.setOnClickListener {
            dismiss()
        }

        view.item_search_key.setText(subject.name)
        view.list_search.layoutManager = LinearLayoutManager(context)
        val adapter = SearchLineAdapter()
        adapter.lines = lineInfoModel.getInfos(subject)
        adapter.setOnItemClickListener { _, _, position ->
            val item = adapter.data[position]
            val exist = adapter.lines?.providers?.firstOrNull { it.site == item.site && it.id == item.id && it.offset == item.offset } != null
            if(exist) {
                showSnackbar(view.list_search, "线路已存在，长按编辑此线路")
                return@setOnItemClickListener
            }
            val lines = (adapter.lines?: LineInfoModel.LineInfoList())
            lines.providers.add(adapter.data[position])
            lineInfoModel.saveInfos(subject, lines)
            adapter.lines = lines
            showLineDialog(null)
            adapter.notifyItemChanged(position)
        }
        adapter.setOnItemLongClickListener { _, _, position ->
            val item = adapter.data[position]//?.let { item -> adapter.lines?.providers?.firstOrNull { it.site == item.site && it.id == item.id }?: item }
            showLineDialog(item)
            dismiss()
            true
        }
        view.list_search.adapter = adapter
        val searchCall = ArrayList<Pair<ProviderAdapter.ProviderInfo, JsEngine.ScriptTask<List<LineInfoModel.LineInfo>>>>()

        view.item_video_api.text = emptyProvider.title
        view.item_video_api.tag = emptyProvider
        updateProvider(view)

        view.item_search.setOnClickListener {
            adapter.setNewData(null)
            val key = view.item_search_key.text.toString()
            searchCall.forEach { it.second.cancel(true) }
            searchCall.clear()

            val jsEngine = App.from(context).jsEngine
            val provider = view.item_video_api.tag as? ProviderAdapter.ProviderInfo?:emptyProvider
            searchCall.addAll(if(provider == emptyProvider){
                ArrayList(context.lineProvider.providerList.values.filter { it.search.isNotEmpty() }).filter { it.search.isNotEmpty() }.map {
                    Pair(it, it.search("search_${it.site}", jsEngine, key))
                }
            }else listOf(provider.let{ Pair(it, it.search("search_${it.site}", jsEngine, key)) }))

            searchCall.forEach {
                it.second.enqueue({lines->
                    adapter.addData(lines)
                }, {e->
                    showSnackbar(view.list_search, "${it.first.title}: ${e.message}")
                })
            }
        }

        val paddingTop = view.bottom_sheet.paddingTop
        val paddingBottom = view.list_search.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.bottom_sheet.setPadding(view.bottom_sheet.paddingLeft, paddingTop + insets.systemWindowInsetTop, view.bottom_sheet.paddingRight, view.bottom_sheet.paddingBottom)
            view.list_search.setPadding(view.list_search.paddingLeft, view.list_search.paddingTop, view.list_search.paddingRight, paddingBottom + insets.systemWindowInsetBottom)
            insets.consumeSystemWindowInsets()
        }

        window?.attributes?.let{
            it.dimAmount = 0.6f
            window?.attributes = it
        }
        window?.setWindowAnimations(R.style.AnimDialog)
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun showSnackbar(view: View, message: String, duration: Int = Snackbar.LENGTH_LONG){
        val snackbar = Snackbar.make(view, message, duration)
        snackbar.view.let {
            it.setPadding(it.paddingLeft, it.paddingTop, it.paddingRight, it.paddingBottom + view.paddingBottom)
        }
        snackbar.show()
    }

    private fun updateProvider(view: View){
        val popList = ListPopupWindow(view.context)
        popList.anchorView = view.item_video_api
        val videoProvider = context.lineProvider
        val providers: ArrayList<ProviderAdapter.ProviderInfo> = ArrayList(videoProvider.providerList.values.filter { it.search.isNotEmpty() })
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
                @Suppress("UNCHECKED_CAST") val info = providers[position] as T
                context.loadProvider(info) {
                    videoProvider.removeProvider(info.site)
                    if (it != null) videoProvider.addProvider(it)
                    updateProvider(view)
                }
                true
            }
        }
        (view.item_video_api?.tag as? LineInfoModel.LineInfo)?.let{ updateInfo(view, it) }
    }
    private fun updateInfo(view: View, info: LineInfoModel.LineInfo){
        val provider = context.lineProvider.getProvider(info.site)?:emptyProvider
        view.item_video_api.text = provider.title
        view.item_video_api.tag = provider
    }

    private val emptyProvider = ProviderAdapter.ProviderInfo("", 0, "所有接口")
}