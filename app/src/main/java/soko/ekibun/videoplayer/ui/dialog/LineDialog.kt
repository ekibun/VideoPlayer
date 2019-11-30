package soko.ekibun.videoplayer.ui.dialog

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
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
import soko.ekibun.videoplayer.model.LineInfoModel
import soko.ekibun.videoplayer.model.VideoProvider
import java.lang.reflect.Type

class LineDialog<T: ProviderAdapter.ProviderInfo>(val context: ProviderAdapter.LineProviderActivity<T>) : Dialog(context, R.style.AppTheme_Dialog) {
    companion object {
        inline fun <reified T: ProviderAdapter.ProviderInfo> showDialog(
            context: ProviderAdapter.LineProviderActivity<*>,
            subject: VideoSubject,
            info: LineInfoModel.LineInfo? = null,
            noinline callback: () -> Unit
        ) {
            showDialog(context, subject, info, callback, object : TypeToken<T>() {}.type, object : TypeToken<List<T>>() {}.type)
        }

        fun showDialog(
            context: ProviderAdapter.LineProviderActivity<*>,
            subject: VideoSubject,
            info: LineInfoModel.LineInfo? = null,
            callback: () -> Unit,
            typeT: Type, typeListT: Type
        ) {
            val dialog = LineDialog(context)
            dialog.subject = subject
            dialog.info = info
            dialog.callback = callback
            dialog.typeT = typeT
            dialog.typeListT = typeListT
            dialog.show()
        }
    }

    lateinit var typeT: Type
    lateinit var typeListT: Type
    lateinit var subject: VideoSubject
    var info: LineInfoModel.LineInfo? = null
    lateinit var callback: () -> Unit
    val emptyProvider = ProviderAdapter.ProviderInfo("", 0, "链接")
    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        val lineInfoModel = App.from(context).lineInfoModel
        val lineInfos = lineInfoModel.getInfos(subject) ?: LineInfoModel.LineInfoList()
        val setProvider = { newInfo: LineInfoModel.LineInfo? ->
            val position =
                lineInfos.providers.indexOfFirst { it.id == info?.id && it.site == info?.site && it.offset == info?.offset }
            when {
                newInfo == null -> if (position >= 0) {
                    lineInfos.providers.removeAt(position)
                    lineInfos.defaultProvider -= if (lineInfos.defaultProvider > position) 1 else 0
                    lineInfos.defaultProvider =
                        Math.max(0, Math.min(lineInfos.providers.size - 1, lineInfos.defaultProvider))
                }
                position >= 0 -> lineInfos.providers[position] = newInfo
                else -> lineInfos.providers.add(newInfo)
            }
            lineInfoModel.saveInfos(subject, lineInfos)
            callback()
        }

        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_add_line, null)
        setContentView(view)

        view.item_delete.visibility = if (info == null) View.GONE else View.VISIBLE
        view.item_delete.setOnClickListener {
            AlertDialog.Builder(context).setMessage("删除这个线路？").setPositiveButton("确定") { _: DialogInterface, _: Int ->
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
            SearchDialog.showDialog(context, subject, callback, typeT, typeListT)
            dismiss()
        }

        view.item_file.setOnClickListener {
            context.loadFile { file ->
                if (file == null) return@loadFile
                updateInfo(view, LineInfoModel.LineInfo("", file))
            }
        }

        view.item_ok.setOnClickListener {
            val provider = view.item_video_api.tag as? ProviderAdapter.ProviderInfo ?: return@setOnClickListener
            setProvider(
                LineInfoModel.LineInfo(
                    provider.site,
                    view.item_video_id.text.toString(),
                    view.item_video_offset.text.toString().toFloatOrNull() ?: 0f,
                    view.item_video_title.text.toString(),
                    view.item_load_danmaku.isEnabled && view.item_load_danmaku.isChecked
                )
            )
            dismiss()
        }
        view.item_outside.setOnClickListener {
            dismiss()
        }

        val paddingBottom = view.item_buttons.paddingBottom
        view.setOnApplyWindowInsetsListener { _, insets ->
            view.item_buttons.setPadding(
                view.item_buttons.paddingLeft,
                view.item_buttons.paddingTop,
                view.item_buttons.paddingRight,
                paddingBottom + insets.systemWindowInsetBottom
            )
            insets.consumeSystemWindowInsets()
        }

        window?.attributes?.let {
            it.dimAmount = 0.6f
            window?.attributes = it
        }
        window?.setWindowAnimations(R.style.AnimDialog)
        window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    val clipboardManager by lazy { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    private fun updateProvider(view: View) {
        val popList = ListPopupWindow(context)
        popList.anchorView = view.item_video_api
        val providerList: ArrayList<ProviderAdapter.ProviderInfo> = ArrayList(context.lineProvider.providerList.values)

        providerList.add(0, emptyProvider)
        providerList.add(ProviderAdapter.ProviderInfo("", 0, "添加..."))
        providerList.add(ProviderAdapter.ProviderInfo("", 0, "导出..."))
        providerList.add(ProviderAdapter.ProviderInfo("", 0, "导入..."))
        popList.setAdapter(ProviderAdapter(context, providerList))
        popList.isModal = true
        view.item_video_api.setOnClickListener {
            popList.show()
            popList.listView?.setOnItemClickListener { _, _, position, _ ->
                popList.dismiss()
                when (position) {
                    providerList.size - 3 -> {
                        //doAdd
                        context.loadProvider(null) {
                            if (it == null) return@loadProvider
                            context.lineProvider.addProvider(it)
                            view.item_video_api.text = it.title
                            view.item_video_api.tag = it
                            updateProvider(view)
                        }
                    }
                    providerList.size - 2 -> {
                        //export
                        clipboardManager.setPrimaryClip(
                            ClipData.newPlainText(
                                "videoplayer.providerInfo",
                                JsonUtil.toJson(context.lineProvider.providerList.values)
                            )
                        )
                        Snackbar.make(view, "数据已导出至剪贴板", Snackbar.LENGTH_LONG).show()
                    }
                    providerList.size - 1 -> {
                        val addProvider = { it: T ->
                            val oldProvider = context.lineProvider.getProvider(it.site)
                            if (oldProvider != null)
                                AlertDialog.Builder(context).setMessage("接口 ${it.title}(${it.site}) 与现有接口 ${oldProvider.title}(${oldProvider.site}) 重复")
                                    .setPositiveButton("替换") { _: DialogInterface, _: Int ->
                                        context.lineProvider.addProvider(it)
                                    }.setNegativeButton("取消") { _: DialogInterface, _: Int -> }.show()
                            else context.lineProvider.addProvider(it)
                        }
                        //inport
                        JsonUtil.toEntity<List<T>>(
                            clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: "", typeListT
                        )?.let { list ->
                            list.forEach { addProvider(it) }
                        } ?: JsonUtil.toEntity<T>(
                            clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: "", typeT
                        )?.let {
                            addProvider(it)
                        } ?: {
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
                if (providerList.size - position < 4 || position == 0) return@setOnItemLongClickListener false
                //edit
                @Suppress("UNCHECKED_CAST") val info = providerList[position] as T
                context.loadProvider(info) {
                    context.lineProvider.removeProvider(info.site)
                    if (it != null) context.lineProvider.addProvider(it)
                    updateProvider(view)
                }
                true
            }
        }
        (view.item_video_api?.tag as? LineInfoModel.LineInfo)?.let { updateInfo(view, it) }
    }

    private fun updateInfo(view: View, info: LineInfoModel.LineInfo) {
        val provider = context.lineProvider.getProvider(info.site) ?: emptyProvider
        view.item_video_api.text = provider.title
        view.item_video_api.tag = provider
        view.item_video_id.setText(info.id)
        view.item_video_offset.setText(info.offset.toString())
        view.item_video_title.setText(info.title)
        view.item_load_danmaku.visibility = if(provider is VideoProvider.ProviderInfo) View.VISIBLE else View.GONE
        view.item_load_danmaku.isEnabled = (provider as? VideoProvider.ProviderInfo)?.hasDanmaku?:false
        view.item_load_danmaku.isChecked = info.loadDanmaku
    }
}