package soko.ekibun.videoplayer.bean

import android.os.Parcel
import android.os.Parcelable
import java.text.DecimalFormat

data class VideoEpisode(
        val site: String? = null,
        val id: String? = null,
        var url: String? = null,
        var cat: String? = null,
        var sort: Float = 0f,
        var name: String? = null,
        var duration: String? = null,
        var airdate: String? = null,
        var comment: Int = 0,
        var desc: String? = null,
        var status: String? = null,
        var progress: String? = null
) : Parcelable {

    fun parseSort(): String{
        return if(cat.isNullOrEmpty() ||  cat == "本篇" )
            "第 ${DecimalFormat("#.##").format(sort)} 话"
        else
            "$cat ${DecimalFormat("#.##").format(sort)}"
    }

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readFloat(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readInt(),
            source.readString(),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(site)
        writeString(id)
        writeString(url)
        writeString(cat)
        writeFloat(sort)
        writeString(name)
        writeString(duration)
        writeString(airdate)
        writeInt(comment)
        writeString(desc)
        writeString(status)
        writeString(progress)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<VideoEpisode> = object : Parcelable.Creator<VideoEpisode> {
            override fun createFromParcel(source: Parcel): VideoEpisode = VideoEpisode(source)
            override fun newArray(size: Int): Array<VideoEpisode?> = arrayOfNulls(size)
        }
    }
}