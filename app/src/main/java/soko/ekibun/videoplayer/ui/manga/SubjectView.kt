package soko.ekibun.videoplayer.ui.manga

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import com.hippo.glgallery.GalleryView
import com.hippo.glgallery.SimpleAdapter
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_manga.*
import kotlinx.android.synthetic.main.subject_detail.*
import kotlinx.android.synthetic.main.subject_episode.*
import kotlinx.android.synthetic.main.subject_infolist.*
import soko.ekibun.util.GlideUtil
import soko.ekibun.util.ResourceUtil
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.model.MangaProvider
import soko.ekibun.videoplayer.model.SubjectProvider.Companion.EXTRA_SUBJECT

class SubjectView(val context: MangaActivity) {
    // val lineAdapter = LineAdapter() // TODO
    val episodeAdapter = SmallEpisodeAdapter(context)
    val emptyView by lazy {
        val view = TextView(context)
        view.text = "点击线路加载剧集信息"
        view.gravity = Gravity.CENTER
        view.height = ResourceUtil.dip2px(context, 65f)
        view
    }

    var galleryProvider: MangaGalleryProvider? = null
    fun createGalleryView(): MangaGalleryProvider {
        galleryProvider?.stop()
        val provider = MangaGalleryProvider(context)
        galleryProvider = provider
        val galleryAdapter = SimpleAdapter(context.item_manga, provider)
        provider.setListener(galleryAdapter)
        provider.setGLRoot(context.item_manga)
        val primaryColor = ResourceUtil.resolveColorAttr(context, R.attr.colorAccent)
        val galleryView = GalleryView.Builder(context, galleryAdapter)
            .setListener(object: GalleryView.Listener{
                override fun onLongPressPage(index: Int) {
                    // TODO
                }

                override fun onTapSliderArea() {
                    context.runOnUiThread {
                        showInfo(true)
                    }
                }

                override fun onTapMenuArea() {
                    // TODO
                }

                override fun onUpdateCurrentIndex(index: Int) {
                    // TODO
                }

            })
            .setLayoutMode(GalleryView.LAYOUT_TOP_TO_BOTTOM)
            .setScaleMode(GalleryView.SCALE_FIT)
            .setStartPosition(GalleryView.START_POSITION_TOP_RIGHT)
            .setEdgeColor(primaryColor and 0xffffff or 0x33000000)
            .setStartPage(0)
            .build()
        context.item_manga.setContentPane(galleryView)
        provider.start()
        return provider
    }

    init {
        context.item_progress.visibility = View.GONE
        context.episodes_line.visibility = View.VISIBLE
        episodeAdapter.emptyView = emptyView
        episodeAdapter.isUseEmpty(true)
        context.episode_list.adapter = episodeAdapter
        context.episode_list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)

        val paddingTop = context.bottom_sheet.paddingTop
        val paddingBottom = context.subject_detail.paddingBottom
        context.root_layout.setOnApplyWindowInsetsListener { _, insets ->
            context.bottom_sheet.setPadding(
                context.bottom_sheet.paddingLeft,
                paddingTop + insets.systemWindowInsetTop,
                context.bottom_sheet.paddingRight,
                context.bottom_sheet.paddingBottom
            )
            context.subject_detail.setPadding(
                context.subject_detail.paddingLeft,
                context.subject_detail.paddingTop,
                context.subject_detail.paddingRight,
                paddingBottom + insets.systemWindowInsetBottom
            )
            insets
        }

        context.item_mask.setOnClickListener {
            showInfo(false)
        }
    }

    fun showInfo(show: Boolean) {
        if (context.item_manga.visibility != View.VISIBLE) return

        context.window.decorView.systemUiVisibility = if (show)
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        else View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY


        context.app_bar.visibility = if (show) View.VISIBLE else View.GONE
        context.item_mask.visibility = if (show) View.VISIBLE else View.GONE
        context.bottom_sheet.visibility = if (show) View.VISIBLE else View.INVISIBLE
        context.bottom_sheet.animation =
            AnimationUtils.loadAnimation(context, if (show) R.anim.move_in_bottom else R.anim.move_out_bottom)
    }

    fun updateEpisode(episodes: List<MangaProvider.MangaEpisode>? = null, exception: Exception? = null) {
        episodeAdapter.setNewData(episodes)
        emptyView.text = when {
            exception != null -> exception.message
            episodes != null -> "这里什么都没有"
            else -> "加载中..."
        }
    }

    private val weekList = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
    @SuppressLint("SetTextI18n")
    fun updateSubject(subject: VideoSubject) {
        if (context.isDestroyed) return
        context.runOnUiThread {
            context.item_detail.setOnClickListener {
                try {
                    val intent =
                        Intent(Intent.ACTION_VIEW, Uri.parse("ekibun://playersubject/${subject.site}/${subject.id}"))
                    intent.putExtra(EXTRA_SUBJECT, subject)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            context.title = subject.name
            //subject detail
            context.item_info.text = subject.type
            context.item_subject_title.text = subject.name
            context.item_air_time.text = "开播时间：${subject.air_date}"
            context.item_air_week.text = {
                var ret = "更新时间："
                subject.air_weekday.toString().forEach {
                    ret += weekList[it.toString().toInt()] + " "
                }
                ret
            }()
            context.item_score.text = subject.rating.toString()
            context.item_score_count.text = "${subject.rating_count}人"
            GlideUtil.with(context.item_cover)
                ?.load(subject.image)
                ?.apply(RequestOptions.errorOf(R.drawable.ic_404).placeholder(context.item_cover.drawable))
                ?.into(context.item_cover)
            GlideUtil.with(context.item_cover_blur)
                ?.load(subject.image)
                ?.apply(
                    RequestOptions.bitmapTransform(
                        BlurTransformation(
                            25,
                            8
                        )
                    ).placeholder(context.item_cover_blur.drawable)
                )
                ?.into(context.item_cover_blur)
            //collection
            context.item_collect_info.text = if (subject.collect.isNullOrEmpty()) "收藏" else subject.collect
            context.item_collect_image.setImageDrawable(
                context.resources.getDrawable(
                    if (subject.collect.isNullOrEmpty()) R.drawable.ic_heart_outline else R.drawable.ic_heart,
                    context.theme
                )
            )
        }
    }
}