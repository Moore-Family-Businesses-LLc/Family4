package com.family4.app.ui.albums

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.data.db.entity.PhotoAlbumEntity
import com.family4.app.databinding.ItemAlbumBinding
import java.text.SimpleDateFormat
import java.util.*

class AlbumsAdapter : ListAdapter<PhotoAlbumEntity, AlbumsAdapter.VH>(DIFF) {

    var onAlbumClick:  ((PhotoAlbumEntity) -> Unit)? = null
    var onDeleteClick: ((PhotoAlbumEntity) -> Unit)? = null

    inner class VH(private val b: ItemAlbumBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(album: PhotoAlbumEntity) {
            b.tvAlbumName.text = album.name
            b.tvAlbumDate.text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                .format(Date(album.createdAt))
            b.root.setOnClickListener { onAlbumClick?.invoke(album) }
            b.btnDeleteAlbum.setOnClickListener { onDeleteClick?.invoke(album) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PhotoAlbumEntity>() {
            override fun areItemsTheSame(a: PhotoAlbumEntity, b: PhotoAlbumEntity) = a.id == b.id
            override fun areContentsTheSame(a: PhotoAlbumEntity, b: PhotoAlbumEntity) = a == b
        }
    }
}
