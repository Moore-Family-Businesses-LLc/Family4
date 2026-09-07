package com.family4.app.ui.board

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.data.db.entity.BoardPostEntity
import com.family4.app.databinding.ItemBoardPostBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BoardPostsAdapter(
    private val onReact: (BoardPostEntity, String) -> Unit,
    private val onPin: (BoardPostEntity) -> Unit,
    private val onDelete: (BoardPostEntity) -> Unit,
    private val formatReactions: (String) -> String
) : ListAdapter<BoardPostEntity, BoardPostsAdapter.PostVH>(DIFF) {

    inner class PostVH(val binding: ItemBoardPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostVH =
        PostVH(ItemBoardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PostVH, position: Int) {
        val post = getItem(position)
        val b = holder.binding

        b.tvAuthor.text = post.authorName
        b.tvContent.text = post.content
        b.tvTime.text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            .format(Date(post.createdAt))
        b.tvReactions.text = formatReactions(post.reactions)
        b.ivPin.isSelected = post.pinned
        b.ivPin.alpha = if (post.pinned) 1f else 0.35f

        b.btnHeart.setOnClickListener { onReact(post, "❤️") }
        b.btnThumb.setOnClickListener { onReact(post, "👍") }
        b.btnLaugh.setOnClickListener { onReact(post, "😂") }
        b.ivPin.setOnClickListener { onPin(post) }
        b.ivDelete.setOnClickListener { onDelete(post) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<BoardPostEntity>() {
            override fun areItemsTheSame(a: BoardPostEntity, b: BoardPostEntity) = a.id == b.id
            override fun areContentsTheSame(a: BoardPostEntity, b: BoardPostEntity) = a == b
        }
    }
}
