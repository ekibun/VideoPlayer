package soko.ekibun.videoplayer.ui.manga

import android.annotation.SuppressLint
import android.content.Intent
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.subject_detail.*
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

    @SuppressLint("SetTextI18n")
    fun init(){
        subjectView.updateSubject(subject)
        subjectProvider.bindService()
        refreshLines()

        context.item_collect.setOnClickListener {
            subjectProvider.updateCollection()
        }
    }

    private fun refreshLines(){
        val infos = lineInfoModel.getInfos(subject)
        val editLines = { info: LineInfoModel.LineInfo? ->
            LineDialog.showDialog<MangaProvider.ProviderInfo>(context, subject, info){ refreshLines() }
        }
        context.runOnUiThread { subjectView.lineAdapter.setNewData(infos?.providers) }
        infos?.let{
            //subjectView.lineAdapter.setNewData(it.providers)
            subjectView.lineAdapter.selectIndex = it.defaultProvider

            subjectView.lineAdapter.setOnItemClickListener { _, _, position ->
                it.defaultProvider = position
                lineInfoModel.saveInfos(subject, it)
                refreshLines()
            }
            subjectView.lineAdapter.onSwitchChange = {position: Int, isCheck: Boolean ->
                it.providers[position].loadDanmaku = isCheck
                lineInfoModel.saveInfos(subject, it)
                refreshLines()
            }
            subjectView.lineAdapter.setOnItemLongClickListener { _, _, position ->
                editLines(it.providers[position])
                true
            }
        }

        context.item_lines.setOnClickListener{
            editLines(null)
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        subjectProvider.onActivityResult(requestCode, resultCode, data)
    }

    fun destroy(){
        context.unbindService(subjectProvider)
    }
}