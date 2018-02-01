package koma.matrix.event.room_message.chat


class LocationInfo(
        val thumbnail_url: String?,
        val thumbnail_info: ThumbnailInfo?
)

class ImageInfo(
        val h:Int,
        val w: Int,
        val mimetype: String,
        val size: Int,
        val thumbnail_url: String?,
        val thumbnail_info: ThumbnailInfo?
)

class AudioInfo(
        val duration: Int?,
        val mimetype: String,
        val size: Int
)

class VideoInfo(
        val h:Int,
        val w: Int,
        val duration: Int?,
        val mimetype: String,
        val size: Int,
        val thumbnail_url: String?,
        val thumbnail_info: ThumbnailInfo?
)

class FileInfo(
        val mimetype: String,
        val size: Int,
        val thumbnail_url: String?,
        val thumbnail_info: ThumbnailInfo?
)

class ThumbnailInfo(
        val h: Int,
        val w: Int,
        val mimetype: String,
        val size: Int
)

class MessageUnsigned(
        /**
         * not used and it keeps changing
         */
        //val age: Int?,
        val transactionId: String?
)
