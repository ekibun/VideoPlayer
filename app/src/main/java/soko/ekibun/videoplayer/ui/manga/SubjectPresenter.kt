package soko.ekibun.videoplayer.ui.manga

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.util.Log
import android.view.View
import android.widget.ListPopupWindow
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import soko.ekibun.videoplayer.ui.view.ScalableLayoutManager
import soko.ekibun.videoplayer.ui.view.pull.PullLoadLayout


class SubjectPresenter(val context: MangaActivity) {
    val subject: VideoSubject get() = subjectProvider.subject
    private val lineInfoModel by lazy { App.from(context).lineInfoModel }
    private val mangaProvider by lazy { App.from(context).mangaProvider }
    private val jsEngine by lazy { App.from(context).jsEngine }
    val subjectView = SubjectView(context)
    val mangaAdapter by lazy { MangaAdapter(context) }


    private val subjectProvider = SubjectProvider(context, object : SubjectProvider.OnChangeListener {
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

    fun refreshSubject() {
        subjectProvider.refreshSubject()
        subjectProvider.refreshEpisode()
    }

    fun init() {
        subjectView.updateSubject(subject)
        subjectProvider.bindService()
        refreshLines()
        val layoutManager = ScalableLayoutManager(context)
        layoutManager.setupWithRecyclerView(context.item_manga) { _, _ ->
            subjectView.showInfo(true)
        }
        context.item_manga.adapter = mangaAdapter

        val dividerItemDecoration = DividerItemDecoration(context, LinearLayoutManager.VERTICAL)
        context.item_manga.addItemDecoration(dividerItemDecoration)

        context.item_collect.setOnClickListener {
            subjectProvider.updateCollection()
        }

        context.item_manga.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                mangaAdapter.data.getOrNull(layoutManager.findFirstVisibleItemPosition())?.ep?.let { context.supportActionBar?.subtitle = it.sort }
            }
        })

        val loadEp = loadEp@{ position: Int, isPrev: Boolean, callback: (Boolean) -> Unit ->
            val ep = subjectView.episodeAdapter.data.getOrNull(position)
            val provider = mangaProvider.getProvider(ep?.site?:"")
            if(ep == null || provider == null) {
                context.item_manga.post{
                    callback(false)
                }
                return@loadEp
            }
            context.item_pull_layout.visibility = View.VISIBLE
            layoutManager.reset()
            provider.getManga("getManga", jsEngine, ep).enqueue({
                if (isPrev) {
                    val curItem = layoutManager.findFirstVisibleItemPosition()
                    val curOffset =
                        layoutManager.findViewByPosition(curItem)?.let { layoutManager.getDecoratedTop(it) } ?: 0
                    mangaAdapter.addData(0, it)
                    layoutManager.scrollToPositionWithOffset(curItem + it.size, curOffset)
                } else mangaAdapter.addData(it)
                callback(true)
            }, {
                callback(false)
            })
        }
        subjectView.episodeAdapter.setOnItemChildClickListener { _, _, position ->
            context.supportActionBar?.subtitle = subjectView.episodeAdapter.data[position].sort
            mangaAdapter.setNewData(null)
            context.item_pull_layout.responseRefresh(false)
            context.item_pull_layout.responseload(false)
            loadEp(position, false) {}
        }
        subjectView.episodeDetailAdapter.setOnItemClickListener { _, _, position ->
            context.supportActionBar?.subtitle = subjectView.episodeDetailAdapter.data[position].sort
            mangaAdapter.setNewData(null)
            context.item_pull_layout.responseRefresh(false)
            context.item_pull_layout.responseload(false)
            subjectView.showEpisodeDetail(false)
            loadEp(position, false) {}
        }

        context.item_pull_layout.setOnPullListener(object : PullLoadLayout.OnPullListener {
            override fun onRefreshStart(pullLayout: PullLoadLayout?) {
                val curIndex = subjectView.episodeAdapter.data.indexOf(mangaAdapter.data.firstOrNull()?.ep)
                loadEp(curIndex-1, true) {
                    context.item_pull_layout.responseRefresh(it)
                }
            }

            override fun onLoadStart(pullLayout: PullLoadLayout?) {
                val curIndex = subjectView.episodeAdapter.data.indexOf(mangaAdapter.data.lastOrNull()?.ep)
                loadEp(curIndex+1, false) {
                    context.item_pull_layout.responseload(it)
                }
            }

        })
    }

    @SuppressLint("SetTextI18n")
    private fun refreshLines() {
        val infos = lineInfoModel.getInfos(subject)
        val editLines = { info: LineInfoModel.LineInfo? ->
            LineDialog.showDialog(
                context,
                subject,
                info,
                object : TypeToken<MangaProvider.ProviderInfo>() {}.type,
                object : TypeToken<List<MangaProvider.ProviderInfo>>() {}.type
            ) { refreshLines() }
        }
        val defaultLine = infos?.getDefaultProvider()
        context.runOnUiThread {
            if (defaultLine != null) {
                val provider = mangaProvider.getProvider(defaultLine.site)
                context.episodes_line_id.text = if (defaultLine.title.isEmpty()) defaultLine.id else defaultLine.title
                context.episodes_line_site.backgroundTintList =
                    ColorStateList.valueOf(((provider?.color ?: 0) + 0xff000000).toInt())
                context.episodes_line_site.visibility = View.VISIBLE
                context.episodes_line_site.text = provider?.title ?: { if (defaultLine.site == "") "线路" else "错误接口" }()
                context.episodes_line.setOnClickListener {
                    val popList = ListPopupWindow(context)
                    popList.anchorView = context.episodes_line
                    val lines = ArrayList(infos.providers)
                    lines.add(LineInfoModel.LineInfo("", "添加线路"))
                    val adapter = LineAdapter(context, lines)
                    adapter.selectIndex = infos.defaultProvider
                    popList.setAdapter(adapter)
                    popList.isModal = true
                    popList.show()
                    popList.listView?.setOnItemClickListener { _, _, position, _ ->
                        popList.dismiss()
                        Log.v("pos", "click: $position")
                        if (position == lines.size - 1) editLines(null)
                        else {
                            infos.defaultProvider = position
                            lineInfoModel.saveInfos(subject, infos)
                            refreshLines()
                        }
                    }
                    popList.listView?.setOnItemLongClickListener { _, _, position, _ ->
                        popList.dismiss()
                        if (position == lines.size - 1) editLines(null)
                        else editLines(lines[position])
                        true
                    }
                }
                // 加载eps
                provider?.let {
                    subjectView.updateEpisode(null)
                    Log.v("load", defaultLine.toString())
                    it.getEpisode("loadEps", jsEngine, defaultLine).enqueue({ eps ->
                        Log.v("load", eps.toString())
                        subjectView.updateEpisode(eps)
                    }, { e ->
                        subjectView.updateEpisode(null, e)
                    })
                }
            } else {
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

    fun destroy() {
        context.unbindService(subjectProvider)
    }
}