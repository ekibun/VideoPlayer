package soko.ekibun.videoplayer.bean

import android.os.Parcel
import android.os.Parcelable

data class VideoSubject(
        val site: String? = null,
        val id: String? = null,
        var url: String? = null,
        var image: String? = null,
        var name: String? = null,
        var type: String? = null,
        var air_date: String? = null,
        var air_weekday: Int = 0,
        var rank: Int = 0,
        var rating: Float = 0f,
        var rating_count: Int = 0,
        var desc: String? = null,
        var eps: List<VideoEpisode>? = null,
        var collect: String? = null,
        var token: String? = null
) : Parcelable {

    constructor(source: Parcel) : this(
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readString(),
            source.readInt(),
            source.readInt(),
            source.readFloat(),
            source.readInt(),
            source.readString(),
            source.createTypedArrayList(VideoEpisode.CREATOR),
            source.readString(),
            source.readString()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(site)
        writeString(id)
        writeString(url)
        writeString(image)
        writeString(name)
        writeString(type)
        writeString(air_date)
        writeInt(air_weekday)
        writeInt(rank)
        writeFloat(rating)
        writeInt(rating_count)
        writeString(desc)
        writeTypedList(eps)
        writeString(collect)
        writeString(token)
    }

    companion object {
        const val BANGUMI_SITE = "bangumi"

        @JvmField
        val CREATOR: Parcelable.Creator<VideoSubject> = object : Parcelable.Creator<VideoSubject> {
            override fun createFromParcel(source: Parcel): VideoSubject = VideoSubject(source)
            override fun newArray(size: Int): Array<VideoSubject?> = arrayOfNulls(size)
        }
    }
}