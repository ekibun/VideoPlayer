package soko.ekibun.videoplayer.ui.video.line

import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ListPopupWindow
import com.google.android.material.snackbar.Snackbar
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.dialog_add_line.view.*
import soko.ekibun.util.JsonUtil
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.model.VideoProvider
import soko.ekibun.videoplayer.ui.video.VideoActivity

class LineDialog(val context: VideoActivity): Dialog(context, R.style.AppTheme_Dialog) {
    companion object {
        fun showDialog(context: VideoActivity, subject: VideoSubject, info: VideoProvider.LineInfo? = null, callback: ()->Unit){
            val dialog = LineDialog(context)
            dialog.subject = subject
            dialog.info = info
            dialog.callback = callback
            dialog.show()
        }
    }

    private fun getKeyBoardHeight(): Int{
        val rect = Rect()
        window?.decorView?.getWindowVisibleDisplayFrame(rect)
        val metrics = DisplayMetrics()
        (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.getMetrics(metrics)
        return metrics.heightPixels - rect.bottom
    }

    val videoProvider by lazy { App.from(context).videoProvider }

    lateinit var subject: VideoSubject
    var info: VideoProvider.LineInfo? = null
    lateinit var callback: ()->Unit
    val emptyProvider = VideoProvider.ProviderInfo("", 0, "链接")
    override fun onCreate(savedInstanceState: Bundle?) {
        val lineInfoModel =  App.from(context).lineInfoModel
        val lineInfos = lineInfoModel.getInfos(subject)?: VideoProvider.LineInfoList()
        val setProvider = { newInfo: VideoProvider.LineInfo? ->
            val position = lineInfos.providers.indexOfFirst { it.id == info?.id && it.site == info?.site && it.offset == info?.offset }
            when {
                newInfo == null -> if(position >= 0){
                    lineInfos.providers.removeAt(position)
                    lineInfos.defaultProvider -= if(lineInfos.defaultProvider > position) 1 else 0
                    lineInfos.defaultProvider = Math.max(0, Math.min(lineInfos.providers.size -1, lineInfos.defaultProvider))
                }
                position >= 0 -> lineInfos.providers[position] = newInfo
                else-> lineInfos.providers.add(newInfo)
            }
            lineInfoModel.saveInfos(subject, lineInfos)
            callback()
        }

        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_line, null)
        setContentView(view)

        view.item_delete.visibility = if(info == null) View.GONE else View.VISIBLE
        view.item_delete.setOnClickListener {
            AlertDialog.Builder(context).setMessage("删除这个线路？").setPositiveButton("确定"){ _: DialogInterface, _: Int ->
                setProvider(null)
                dismiss()
            }.show()
        }

        view.item_video_api.text = emptyProvider.title
        view.item_video_api.tag = emptyProvider
        view.item_load_danmaku.isChecked = false
        view.item_load_danmaku.isEnabled = false
        updateProvider(view)
        info?.let { updateInfo(view, it) }

        view.item_search.setOnClickListener {
            SearchDialog.showDialog(context, subject, callback)
            dismiss()
        }

        view.item_file.setOnClickListener {
            context.loadFile {file->
                if(file== null) return@loadFile
                updateInfo(view, VideoProvider.LineInfo("", file))
            }
        }

        view.item_ok.setOnClickListener {
            val provider = view.item_video_api.tag as? VideoProvider.ProviderInfo?:return@setOnClickListener
            setProvider(VideoProvider.LineInfo(provider.site, view.item_video_id.text.toString(),
                view.item_video_offset.text.toString().toFloatOrNull() ?: 0f,
                view.item_video_title.text.toString(), view.item_load_danmaku.isEnabled && view.item_load_danmaku.isChecked))
            dismiss()
        }

        window?.decorView?.viewTreeObserver?.addOnGlobalLayoutListener{
            view.item_keyboard.layoutParams?.let{
                it.height = getKeyBoardHeight()
                view.item_keyboard.layoutParams = it
            }
        }
        view.item_outside.setOnClickListener {
            dismiss()
        }
        window?.attributes?.let{
            it.dimAmount = 0.6f
            window?.attributes = it
        }
        window?.setWindowAnimations(R.style.AnimDialog)
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    val clipboardManager by lazy { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    private fun updateProvider(view: View){
        val popList = ListPopupWindow(context)
        popList.anchorView = view.item_video_api
        val providerList = ArrayList(videoProvider.providerList.values)

        providerList.add(0, emptyProvider)
        providerList.add(VideoProvider.ProviderInfo("", 0, "添加..."))
        providerList.add(VideoProvider.ProviderInfo("", 0, "导出..."))
        providerList.add(VideoProvider.ProviderInfo("", 0, "导入..."))
        popList.setAdapter(ProviderAdapter(context, providerList))
        popList.isModal = true
        view.item_video_api.setOnClickListener {
            popList.show()
            popList.listView?.setOnItemClickListener { _, _, position, _ ->
                popList.dismiss()
                when(position){
                    providerList.size - 3 -> {
                        //doAdd
                        context.loadProvider(null) {
                            if (it == null) return@loadProvider
                            videoProvider.addProvider(it)
                            view.item_video_api.text = it.title
                            view.item_video_api.tag = it
                            updateProvider(view)
                        }
                    }
                    providerList.size - 2 -> {
                        //export
                        clipboardManager.primaryClip = ClipData.newPlainText("videoplayer.providerInfo", JsonUtil.toJson(videoProvider.providerList.values))
                        Snackbar.make(view, "数据已导出至剪贴板", Snackbar.LENGTH_LONG).show()
                    }
                    providerList.size - 1 -> {
                        val addProvider = { it: VideoProvider.ProviderInfo ->
                            val oldProvider = videoProvider.getProvider(it.site)
                            if(oldProvider != null)
                                AlertDialog.Builder(context).setMessage("接口 ${it.title}(${it.site}) 与现有接口 ${oldProvider.title}(${oldProvider.site}) 重复")
                                    .setPositiveButton("替换"){ _: DialogInterface, _: Int ->
                                        videoProvider.addProvider(it)
                                    }.setNegativeButton("取消"){ _: DialogInterface, _: Int -> }.show()
                            else videoProvider.addProvider(it)
                        }
                        //inport
                        JsonUtil.toEntity<List<VideoProvider.ProviderInfo>>(clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()?:"", object: TypeToken<List<VideoProvider.ProviderInfo>>(){}.type)?.let{
                            it.forEach { addProvider(it) }
                        }?:JsonUtil.toEntity(clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()?:"", VideoProvider.ProviderInfo::class.java)?.let{
                            addProvider(it)
                        }?:{
                            Snackbar.make(view, "剪贴板没有数据", Snackbar.LENGTH_LONG).show()
                        }()
                    }
                    else -> {
                        val provider = providerList[position]
                        view.item_video_api.text = provider.title
                        view.item_video_api.tag = provider
                    }
                }
            }
            popList.listView?.setOnItemLongClickListener { _, _, position, _ ->
                popList.dismiss()
                if(providerList.size - position < 4 || position == 0) return@setOnItemLongClickListener false
                //edit
                val info = providerList[position]
                context.loadProvider(providerList[position]) {
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
        val provider = App.from(context).videoProvider.getProvider(info.site)?:emptyProvider
        view.item_video_api.text = provider.title
        view.item_video_api.tag = provider
        view.item_video_id.setText(info.id)
        view.item_video_offset.setText(info.offset.toString())
        view.item_video_title.setText(info.title)
        view.item_load_danmaku.isEnabled = provider.hasDanmaku
        view.item_load_danmaku.isChecked = info.loadDanmaku
    }
}