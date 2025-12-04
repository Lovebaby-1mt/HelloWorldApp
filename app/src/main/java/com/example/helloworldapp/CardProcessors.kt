package com.example.helloworldapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson

/**
 * 具体的卡片处理器实现文件
 * 已修改：所有 Processor 构造函数增加 layoutResId 参数
 */

// ==========================================
// 1. 图文卡片 (ImageCard)
// ==========================================
data class ImageCardData(
    val author: String,
    val time: String,
    val content: String,
    val imageSource: Any,
    val layoutType: Int = 1
)

// 修复：构造函数增加 layoutResId
class ImageCardProcessor(
    private val layoutResId: Int = R.layout.item_card_image, // 新增参数
    private val onImageCardClick: () -> Unit
) : CardProcessor {

    override fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        return ImageCardViewHolder(view)
    }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: Feedable, onLongClick: ((Int) -> Unit)?) {
        if (holder is ImageCardViewHolder) {
            val data = Gson().fromJson(item.data, ImageCardData::class.java)
            holder.bind(data, onImageCardClick, onLongClick)
        }
    }

    class ImageCardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvAuthor: TextView? = view.findViewById(R.id.tv_author)
        private val tvTime: TextView? = view.findViewById(R.id.tv_time)
        private val tvContent: TextView? = view.findViewById(R.id.tv_content)
        private val ivContentImage: ImageView? = view.findViewById(R.id.iv_content_image)

        fun bind(item: ImageCardData, onClick: () -> Unit, onLongClick: ((Int) -> Unit)?) {
            tvAuthor?.text = item.author
            tvTime?.text = item.time
            tvContent?.text = item.content

            val imgSource = item.imageSource
            val finalSource = if (imgSource is Double) imgSource.toInt() else imgSource

            if (ivContentImage != null) {
                Glide.with(itemView.context)
                    .load(finalSource)
                    .placeholder(R.drawable.ic_menu_recent_history)
                    .error(R.drawable.bg_cloudy)
                    .into(ivContentImage)
            }

            itemView.setOnClickListener { onClick() }
            itemView.setOnLongClickListener {
                onLongClick?.invoke(adapterPosition)
                true
            }
        }
    }
}

// ==========================================
// 2. 文章卡片 (ArticleCard)
// ==========================================
data class ArticleCardData(
    val title: String,
    val summary: String,
    val time: String,
    val author: String,
    val layoutType: Int = 1
)

// 修复：构造函数增加 layoutResId
class ArticleCardProcessor(
    private val layoutResId: Int = R.layout.item_card_article // 新增参数
) : CardProcessor {

    override fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        return ArticleCardViewHolder(view)
    }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: Feedable, onLongClick: ((Int) -> Unit)?) {
        if (holder is ArticleCardViewHolder) {
            val data = Gson().fromJson(item.data, ArticleCardData::class.java)
            holder.bind(data, onLongClick)
        }
    }

    class ArticleCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView? = itemView.findViewById(R.id.tv_article_title)
        private val tvSummary: TextView? = itemView.findViewById(R.id.tv_article_summary)
        private val tvTime: TextView? = itemView.findViewById(R.id.tv_article_time)
        private val tvAuthor: TextView? = itemView.findViewById(R.id.tv_author)

        fun bind(data: ArticleCardData, onLongClick: ((Int) -> Unit)?) {
            tvTitle?.text = data.title
            tvSummary?.text = data.summary
            tvTime?.text = data.time
            tvAuthor?.text = data.author

            itemView.setOnClickListener {
                Toast.makeText(itemView.context, "阅读文章: ${data.title}", Toast.LENGTH_SHORT).show()
            }
            itemView.setOnLongClickListener {
                onLongClick?.invoke(adapterPosition)
                true
            }
        }
    }
}

// ==========================================
// 3. 视频卡片 (VideoCard)
// ==========================================
data class VideoCardData(
    val title: String,
    val duration: String,
    val coverImage: Any,
    val author: String,
    var time: String,
    val layoutType: Int = 1
)

// 修复：构造函数增加 layoutResId
class VideoCardProcessor(
    private val layoutResId: Int = R.layout.item_card_video // 新增参数
) : CardProcessor {

    override fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        return VideoCardViewHolder(view)
    }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: Feedable, onLongClick: ((Int) -> Unit)?) {
        if (holder is VideoCardViewHolder) {
            val data = Gson().fromJson(item.data, VideoCardData::class.java)
            holder.bind(data, onLongClick)
        }
    }

    class VideoCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView? = itemView.findViewById(R.id.tv_video_title)
        private val tvDuration: TextView? = itemView.findViewById(R.id.tv_video_duration)
        private val ivCover: ImageView? = itemView.findViewById(R.id.iv_video_cover)
        private val tvTime: TextView? = itemView.findViewById(R.id.tv_time)
        private val tvAuthor: TextView? = itemView.findViewById(R.id.tv_author)

        fun bind(data: VideoCardData, onLongClick: ((Int) -> Unit)?) {
            tvTitle?.text = data.title
            tvDuration?.text = data.duration
            tvAuthor?.text = data.author
            tvTime?.text = data.time

            val imgSource = data.coverImage
            val finalSource = if (imgSource is Double) imgSource.toInt() else imgSource

            if (ivCover != null) {
                Glide.with(itemView.context).load(finalSource).into(ivCover)
            }

            itemView.setOnClickListener {
                Toast.makeText(itemView.context, "播放视频: ${data.title}", Toast.LENGTH_SHORT).show()
            }
            itemView.setOnLongClickListener {
                onLongClick?.invoke(adapterPosition)
                true
            }
        }
    }
}

// ==========================================
// 4. 广告卡片 (AdCard)
// ==========================================
data class AdCardData(
    val title: String,
    val desc: String,
    val coverImage: Any,
    val buttonText: String,
    val layoutType: Int = 1
)

// 修复：构造函数增加 layoutResId
class AdCardProcessor(
    private val layoutResId: Int = R.layout.item_card_ad // 新增参数
) : CardProcessor {

    override fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        return AdCardViewHolder(view)
    }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: Feedable, onLongClick: ((Int) -> Unit)?) {
        if (holder is AdCardViewHolder) {
            val data = Gson().fromJson(item.data, AdCardData::class.java)
            holder.bind(data, onLongClick)
        }
    }

    class AdCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView? = itemView.findViewById(R.id.tv_ad_title)
        private val tvDesc: TextView? = itemView.findViewById(R.id.tv_ad_desc)
        private val btnAction: Button? = itemView.findViewById(R.id.btn_ad_action)
        private val ivAdImage: ImageView? = itemView.findViewById(R.id.iv_ad_image)

        fun bind(data: AdCardData, onLongClick: ((Int) -> Unit)?) {
            tvTitle?.text = data.title
            tvDesc?.text = data.desc
            btnAction?.text = data.buttonText

            val imgSource = data.coverImage
            val finalSource = if (imgSource is Double) imgSource.toInt() else imgSource

            if (ivAdImage != null) {
                Glide.with(itemView.context).load(finalSource).into(ivAdImage)
            }

            btnAction?.setOnClickListener {
                Toast.makeText(itemView.context, "点击广告: ${data.title}", Toast.LENGTH_SHORT).show()
            }
            itemView.setOnLongClickListener {
                onLongClick?.invoke(adapterPosition)
                true
            }
        }
    }
}
