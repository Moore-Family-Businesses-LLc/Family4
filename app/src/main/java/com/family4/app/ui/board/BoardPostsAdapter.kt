package com.family4.app.ui.board

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.data.db.entity.BoardPostEntity
import com.family4.app.databinding.ItemBoardPostBinding

class BoardPostsAdapter(
    private val onReact: (BoardPostEntity, String) -> Unit,
    private val onPin: (BoardPostEntity) -> Unit,
    private val onDelete: (BoardPostEntity) -> Unit,
    private val viewModel: FamilyBoardViewModel
) : ListAdapter<BoardPostEntity, BoardPostsAdapter.PostVH>(DIFF) {

    inner class PostVH(val binding: ItemBoardPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostVH =
        PostVH(ItemBoardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: PostVH, position: Int) {
        val post = getItem(position)
        val b = holder.binding

        // ── Author + Avatar ───────────────────────────────────────────────
        b.tvAuthor.text = post.authorName
        val initials = viewModel.initials(post.authorName)
        val avatarColor = viewModel.avatarColor(post.authorId)
        b.tvAvatar.text = initials
        b.tvAvatar.background.mutate().setTint(avatarColor)

        // ── Relative timestamp ────────────────────────────────────────────
        b.tvTime.text = viewModel.formatRelativeTime(post.createdAt)

        // ── Post type badge & color strip ─────────────────────────────────
        val (typeEmoji, typeLabel) = viewModel.postTypeLabel(post.postType)
        b.tvPostType.text = "$typeEmoji $typeLabel"
        val typeColor = viewModel.postTypeColor(post.postType)
        b.postTypeStrip.setBackgroundColor(typeColor)
        b.tvPostType.setTextColor(typeColor)

        // ── Content ───────────────────────────────────────────────────────
        b.tvContent.text = post.content

        // ── Pin state ─────────────────────────────────────────────────────
        b.ivPin.isSelected = post.pinned
        b.ivPin.alpha = if (post.pinned) 1f else 0.35f

        // ── Reactions ─────────────────────────────────────────────────────
        b.tvReactions.text = viewModel.formatReactions(post.reactions)

        // ── Clicks ───────────────────────────────────────────────────────
        b.btnHeart.setOnClickListener { onReact(post, "❤️") }
        b.btnThumb.setOnClickListener { onReact(post, "👍") }
        b.btnLaugh.setOnClickListener { onReact(post, "😂") }
        b.btnFire.setOnClickListener { onReact(post, "🔥") }
        b.btnStar.setOnClickListener { onReact(post, "⭐") }
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
