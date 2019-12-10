package soko.ekibun.videoplayer.ui.manga

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.util.Log
import android.view.View
import android.widget.ListPopupWindow
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_manga.*
import kotlinx.android.synthetic.main.subject_detail.*
import kotlinx.android.synthetic.main.subject_episode.*
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.model.LineInfoModel
import soko.ekibun.videoplayer.model.MangaProvider
import soko.ekibun.videoplayer.model.SubjectProvider
import soko.ekibun.videoplayer.ui.dialog.LineDialog


class SubjectPresenter(val context: MangaActivity) {
    val subject: VideoSubject get() = subjectProvider.subject
    private val lineInfoModel by lazy{ App.from(context).lineInfoModel }
    private val mangaProvider by lazy{ App.from(context).mangaProvider }
    private val jsEngine by lazy { App.from(context).jsEngine }
    val subjectView = SubjectView(context)


    private val subjectProvider = SubjectProvider(context, object: SubjectProvider.OnChangeListener {
        override fun onSubjectSeasonChange(seasons: List<VideoSubject>) {
            // TODO
        }

        override fun onSubjectChange(subject: VideoSubject) {
            subjectView.updateSubject(subject)
        }
        override fun onEpisodeListChange(eps: List<VideoEpisode>, merge: Boolean) {
            // TODO
        }
    })

    fun refreshSubject(){
        subjectProvider.refreshSubject()
        subjectProvider.refreshEpisode()
    }

    fun init(){
        subjectView.updateSubject(subject)
        subjectProvider.bindService()
        refreshLines()

        context.item_collect.setOnClickListener {
            subjectProvider.updateCollection()
        }
        subjectView.episodeAdapter.setOnItemChildClickListener { _, _, position ->
            val ep = subjectView.episodeAdapter.data[position]
            val provider = mangaProvider.getProvider(ep.site)?: return@setOnItemChildClickListener
            context.item_manga.visibility = View.VISIBLE
            val galleryProvider = subjectView.createGalleryView()
            provider.getManga("getManga", jsEngine, ep).enqueue({
                galleryProvider.data.clear()
                galleryProvider.data.addAll(it)
                galleryProvider.notifyDataChanged()
            }, {
                Log.v("req err", it.toString())
            })
        }
    }

    @SuppressLint("SetTextI18n")
    private fun refreshLines(){
        val infos = lineInfoModel.getInfos(subject)
        val editLines = { info: LineInfoModel.LineInfo? ->
            LineDialog.showDialog(context, subject, info, object : TypeToken<MangaProvider.ProviderInfo>() {}.type, object : TypeToken<List<MangaProvider.ProviderInfo>>() {}.type){ refreshLines() }
        }
        val defaultLine = infos?.getDefaultProvider()
        context.runOnUiThread {
            if(defaultLine != null) {
                val provider = mangaProvider.getProvider(defaultLine.site)
                context.episodes_line_id.text = if(defaultLine.title.isEmpty()) defaultLine.id else defaultLine.title
                context.episodes_line_site.backgroundTintList = ColorStateList.valueOf(((provider?.color?:0) + 0xff000000).toInt())
                context.episodes_line_site.visibility = View.VISIBLE
                context.episodes_line_site.text = provider?.title?:{ if(defaultLine.site == "") "线路" else "错误接口" }()
                context.episodes_line.setOnClickListener {
                    val popList = ListPopupWindow(context)
                    popList.anchorView = context.episodes_line
                    val lines = ArrayList(infos.providers)
                    lines.add(LineInfoModel.LineInfo("", "添加线路"))
                    popList.setAdapter(LineAdapter(context, lines))
                    popList.isModal = true
                    popList.show()
                    popList.listView?.setOnItemClickListener { _, _, position, _ ->
                        popList.dismiss()
                        Log.v("pos", "click: $position")
                        if(position == lines.size - 1) editLines(null)
                        else{
                            infos.defaultProvider = position
                            lineInfoModel.saveInfos(subject, infos)
                            refreshLines()
                        }
                    }
                    popList.listView?.setOnItemLongClickListener { _, _, position, _ ->
                        popList.dismiss()
                        if(position == lines.size - 1) editLines(null)
                        else editLines(lines[position])
                        true
                    }
                }
                // 加载eps
                provider?.let {
                    subjectView.updateEpisode(null)
                    Log.v("load", defaultLine.toString())
                    it.getEpisode("loadEps", jsEngine, defaultLine).enqueue({eps ->
                        Log.v("load", eps.toString())
                        subjectView.updateEpisode(eps)
                    }, {e ->
                        subjectView.updateEpisode(null, e)
                    })
                }
            }else{
                context.episodes_line_site.visibility = View.GONE
                context.episodes_line_id.text = "+ 添加线路"
                context.episodes_line.setOnClickListener {
                    editLines(null)
                }
            }

        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        subjectProvider.onActivityResult(requestCode, resultCode, data)
    }

    fun destroy(){
        context.unbindService(subjectProvider)
    }
}