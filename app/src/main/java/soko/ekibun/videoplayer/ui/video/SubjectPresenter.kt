package soko.ekibun.videoplayer.ui.video

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.util.Log
import com.google.android.exoplayer2.offline.DownloadHelper
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.item_episode.view.*
import kotlinx.android.synthetic.main.subject_detail.*
import soko.ekibun.videoplayer.App
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.bean.VideoCache
import soko.ekibun.videoplayer.bean.VideoEpisode
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.model.SubjectProvider
import soko.ekibun.videoplayer.service.DownloadService
import soko.ekibun.videoplayer.ui.video.line.LineDialog
import java.io.IOException

class SubjectPresenter(val context: VideoActivity) {
    val subject: VideoSubject get() = subjectProvider.subject
    private val lineInfoModel by lazy{ App.from(context).lineInfoModel }
    val subjectView = SubjectView(context)
    private val subjectProvider = SubjectProvider(context, object: SubjectProvider.OnChangeListener {
        override fun onSubjectSeasonChange(seasons: List<VideoSubject>) {
            if(seasons.size > 1) context.runOnUiThread {
                subjectView.seasonAdapter.setNewData(seasons)
                subjectView.seasonAdapter.currentId = subject.id ?: ""
                subjectView.seasonLayoutManager.scrollToPositionWithOffset( subjectView.seasonAdapter.data.indexOfFirst { it.id == subject.id }, 0)
            }
        }

        override fun onSubjectChange(subject: VideoSubject) {
            subjectView.updateSubject(subject)
        }
        override fun onEpisodeListChange(eps: List<VideoEpisode>, merge: Boolean) {
            subjectView.updateEpisode(eps, subject, merge)
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

        subjectView.seasonAdapter.setOnItemClickListener { _, _, position ->
            val subject = subjectView.seasonAdapter.data[position]
            if(subject.id != subjectView.seasonAdapter.currentId)
                VideoActivity.startActivity(context, subject)
        }

        subjectView.episodeAdapter.setOnItemChildClickListener { _, _, position ->
            context.videoPresenter.playEpisode(subjectView.episodeAdapter.data[position])
        }

        subjectView.episodeAdapter.setOnItemChildLongClickListener { _, _, position ->
            subjectProvider.updateProgress(subjectView.episodeAdapter.data.subList(0, position + 1))
            true
        }

        subjectView.episodeDetailAdapter.setOnItemClickListener { _, _, position ->
            context.videoPresenter.playEpisode(subjectView.episodeDetailAdapter.data[position].t)
        }

        subjectView.episodeDetailAdapter.setOnItemLongClickListener { _, _, position ->
            subjectProvider.updateProgress(subjectView.episodeDetailAdapter.data.subList(0, position + 1).mapNotNull { it.t })
            true
        }

        subjectView.episodeDetailAdapter.setOnItemChildClickListener { _, _, position ->
            val item = subjectView.episodeDetailAdapter.data[position]
            val info = lineInfoModel.getInfos(subject)?.getDefaultProvider()?:return@setOnItemChildClickListener
            subjectView.episodeDetailAdapter.getViewByPosition(context.episode_detail_list, position, R.id.item_layout)?.let{
                it.item_download_info.text = "获取视频信息"
            }
            context.videoPresenter.videoModel.getVideo(item.t.parseSort(), item.t, subject, info, {videoInfo, error->
                subjectView.episodeDetailAdapter.getViewByPosition(context.episode_detail_list, position, R.id.item_layout)?.let{
                    it.item_download_info.text = if(videoInfo != null)"解析视频地址" else "获取视频信息出错：${error?.message}"
                }
            }, {request, _, error ->
                subjectView.episodeDetailAdapter.getViewByPosition(context.episode_detail_list, position, R.id.item_layout)?.let{
                    it.item_download_info.text = "解析视频地址出错：${error?.message}"
                    if(request == null || request.url.startsWith("/")) return@let
                    it.item_download_info.text = "创建视频请求"
                    context.videoPresenter.videoModel.createDownloadRequest(request, object: DownloadHelper.Callback{
                        override fun onPrepared(helper: DownloadHelper) {
                            val downloadRequest = helper.getDownloadRequest(request.url, null)
                            Log.v("downloadRequest", downloadRequest.streamKeys.toString())
                            DownloadService.download(context, item.t, subject, VideoCache(item.t, downloadRequest.type, downloadRequest.streamKeys, request))
                        }
                        override fun onPrepareError(helper: DownloadHelper, e: IOException) {
                            it.item_download_info.post { it.item_download_info.text = e.toString() }
                        }
                    })
                }
            }, {
                AlertDialog.Builder(context).setMessage("正在使用非wifi网络").setPositiveButton("继续缓存"){_, _ -> it() }
                    .setNegativeButton("取消"){_, _ -> subjectView.episodeDetailAdapter.notifyItemChanged(position) }
                    .setOnDismissListener { subjectView.episodeDetailAdapter.notifyItemChanged(position) }.show()
            })
        }

        subjectView.episodeDetailAdapter.setOnItemChildLongClickListener { _, _, position ->
            val item = subjectView.episodeDetailAdapter.data[position]
            val videoCache = App.from(context).videoCacheModel.getVideoCache(item.t, subject)
            if(videoCache != null) DownloadService.remove(context, item.t, subject)
            true
        }

    }

    private fun refreshLines(){
        val infos = lineInfoModel.getInfos(subject)
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
                LineDialog.showDialog(context, subject, it.providers[position]){ refreshLines() }
                true
            }
        }

        context.item_lines.setOnClickListener{
            LineDialog.showDialog(context, subject){ refreshLines() }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        subjectProvider.onActivityResult(requestCode, resultCode, data)
    }

    fun destroy(){
        context.unbindService(subjectProvider)
    }
}