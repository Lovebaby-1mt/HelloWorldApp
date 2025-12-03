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
 * 已移除 TextCardProcessor
 */

// --- 1. 图文卡片处理器 (原 FeedItem) ---

data class ImageCardData(
    val author: String,
    val time: String,
    val content: String,
    val imageSource: Any
)

class ImageCardProcessor(private val onImageCardClick: () -> Unit) : CardProcessor {
    
    override fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            // 使用统一命名后的布局文件 item_card_image
            .inflate(R.layout.item_card_image, parent, false)
        return ImageCardViewHolder(view)
    }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: Feedable) {
        if (holder is ImageCardViewHolder) {
            val data = Gson().fromJson(item.data, ImageCardData::class.java)
            holder.bind(data, onImageCardClick)
        }
    }

    class ImageCardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvAuthor: TextView = view.findViewById(R.id.tv_author)
        private val tvTime: TextView = view.findViewById(R.id.tv_time)
        private val tvContent: TextView = view.findViewById(R.id.tv_content)
        private val ivContentImage: ImageView = view.findViewById(R.id.iv_content_image)
        private val btnLike: View = view.findViewById(R.id.ll_like)
        private val btnComment: View = view.findViewById(R.id.ll_comment)
        private val btnShare: View = view.findViewById(R.id.ll_share)

        fun bind(item: ImageCardData, onClick: () -> Unit) {
            tvAuthor.text = item.author
            tvTime.text = item.time
            tvContent.text = item.content
            
            val imgSource = item.imageSource
            val finalSource = if (imgSource is Double) imgSource.toInt() else imgSource

            Glide.with(itemView.context)
                .load(finalSource)
                .placeholder(R.drawable.ic_menu_recent_history)
                .error(R.drawable.bg_cloudy)
                .into(ivContentImage)

            itemView.setOnClickListener { onClick() }
            
            btnLike.setOnClickListener { 
                Toast.makeText(itemView.context, "点赞", Toast.LENGTH_SHORT).show() 
            }
            btnComment.setOnClickListener {
                Toast.makeText(itemView.context, "评论", Toast.LENGTH_SHORT).show()
            }
            btnShare.setOnClickListener {
                Toast.makeText(itemView.context, "分享", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// --- 2. 文章卡片处理器 ---

data class ArticleCardData(
    val title: String,
    val summary: String,
    val time: String,
    val author: String
)

class ArticleCardProcessor : CardProcessor {
    override fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_article, parent, false)
        return ArticleCardViewHolder(view)
    }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: Feedable) {
        if (holder is ArticleCardViewHolder) {
            val data = Gson().fromJson(item.data, ArticleCardData::class.java)
            holder.bind(data)
        }
    }

    class ArticleCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_article_title)
        private val tvSummary: TextView = itemView.findViewById(R.id.tv_article_summary)
        private val tvTime: TextView = itemView.findViewById(R.id.tv_article_time)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tv_author) // 新增

        fun bind(data: ArticleCardData) {
            tvTitle.text = data.title
            tvSummary.text = data.summary
            tvTime.text = data.time
            tvAuthor.text = data.author // 绑定作者
            
            itemView.setOnClickListener {
                Toast.makeText(itemView.context, "阅读文章: ${data.title}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// --- 3. 视频卡片处理器 ---

data class VideoCardData(
    val title: String,
    val duration: String,
    val coverImage: Any,
    val author: String // 新增：作者名
)

class VideoCardProcessor : CardProcessor {
    override fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_video, parent, false)
        return VideoCardViewHolder(view)
    }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: Feedable) {
        if (holder is VideoCardViewHolder) {
            val data = Gson().fromJson(item.data, VideoCardData::class.java)
            holder.bind(data)
        }
    }

    class VideoCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_video_title)
        private val tvDuration: TextView = itemView.findViewById(R.id.tv_video_duration)
        private val ivCover: ImageView = itemView.findViewById(R.id.iv_video_cover)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tv_author) // 新增

        fun bind(data: VideoCardData) {
            tvTitle.text = data.title
            tvDuration.text = data.duration
            tvAuthor.text = data.author // 绑定作者
            
            val imgSource = data.coverImage
            val finalSource = if (imgSource is Double) imgSource.toInt() else imgSource

            Glide.with(itemView.context)
                .load(finalSource)
                .centerCrop()
                .into(ivCover)

            itemView.setOnClickListener {
                Toast.makeText(itemView.context, "播放视频: ${data.title}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// --- 4. 广告卡片处理器 ---

data class AdCardData(
    val title: String,
    val desc: String,
    val coverImage: Any,
    val buttonText: String
)

class AdCardProcessor : CardProcessor {
    override fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_card_ad, parent, false)
        return AdCardViewHolder(view)
    }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: Feedable) {
        if (holder is AdCardViewHolder) {
            val data = Gson().fromJson(item.data, AdCardData::class.java)
            holder.bind(data)
        }
    }

    class AdCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_ad_title)
        private val tvDesc: TextView = itemView.findViewById(R.id.tv_ad_desc)
        private val btnAction: Button = itemView.findViewById(R.id.btn_ad_action)
        private val ivAdImage: ImageView = itemView.findViewById(R.id.iv_ad_image)

        fun bind(data: AdCardData) {
            tvTitle.text = data.title
            tvDesc.text = data.desc
            btnAction.text = data.buttonText
            
            val imgSource = data.coverImage
            val finalSource = if (imgSource is Double) imgSource.toInt() else imgSource

            Glide.with(itemView.context)
                .load(finalSource)
                .centerCrop()
                .into(ivAdImage)

            btnAction.setOnClickListener {
                Toast.makeText(itemView.context, "点击广告: ${data.title}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}