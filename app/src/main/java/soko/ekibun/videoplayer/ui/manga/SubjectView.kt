package soko.ekibun.videoplayer.ui.manga

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_manga.*
import kotlinx.android.synthetic.main.subject_detail.*
import soko.ekibun.videoplayer.R
import soko.ekibun.videoplayer.bean.VideoSubject
import soko.ekibun.videoplayer.model.SubjectProvider.Companion.EXTRA_SUBJECT
import soko.ekibun.videoplayer.ui.video.LineAdapter

class SubjectView(val context: MangaActivity) {
    val lineAdapter = LineAdapter() // TODO

    init {
        context.line_list.adapter = lineAdapter
        context.line_list.layoutManager = LinearLayoutManager(context)

        val behavior = BottomSheetBehavior.from(context.bottom_sheet)
        behavior.setBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback(){
            @SuppressLint("SwitchIntDef")
            override fun onStateChanged(bottomSheet: View, @BottomSheetBehavior.State newState: Int) {
                context.app_bar.visibility = if(newState == BottomSheetBehavior.STATE_EXPANDED) View.VISIBLE else View.INVISIBLE
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
        //behavior.isHideable = true

        val paddingTop = context.bottom_sheet.paddingTop
        val paddingBottom = context.subject_detail.paddingBottom
        context.root_layout.setOnApplyWindowInsetsListener { _, insets ->
            context.bottom_sheet.setPadding(context.bottom_sheet.paddingLeft, paddingTop + insets.systemWindowInsetTop, context.bottom_sheet.paddingRight, context.bottom_sheet.paddingBottom)
            context.subject_detail.setPadding(context.subject_detail.paddingLeft, context.subject_detail.paddingTop, context.subject_detail.paddingRight, paddingBottom + insets.systemWindowInsetBottom)
            insets
        }
    }

    private val weekList = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
    @SuppressLint("SetTextI18n")
    fun updateSubject(subject: VideoSubject){
        if(context.isDestroyed) return
        context.runOnUiThread {
            context.item_detail.setOnClickListener {
                try{
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("ekibun://playersubject/${subject.site}/${subject.id}"))
                    intent.putExtra(EXTRA_SUBJECT, subject)
                    context.startActivity(intent)
                }catch (e: Exception){ e.printStackTrace() }
            }

            context.toolbar_bottom.title = subject.name
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
            Glide.with(context.item_cover)
                .applyDefaultRequestOptions(RequestOptions.placeholderOf(context.item_cover.drawable))
                .load(subject.image)
                .apply(RequestOptions.errorOf(R.drawable.ic_404))
                .into(context.item_cover)
            Glide.with(context.item_cover_blur)
                .applyDefaultRequestOptions(RequestOptions.placeholderOf(context.item_cover_blur.drawable))
                .load(subject.image)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 8)))
                .into(context.item_cover_blur)
            //collection
            context.item_collect_info.text = if(subject.collect.isNullOrEmpty()) "收藏" else subject.collect
            context.item_collect_image.setImageDrawable(context.resources.getDrawable(
                if(subject.collect.isNullOrEmpty()) R.drawable.ic_heart_outline else R.drawable.ic_heart, context.theme))
        }
    }
}