package com.family4.app.ui.albums

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.family4.app.data.db.entity.PhotoEntity
import com.family4.app.databinding.ItemPhotoBinding

class PhotosAdapter(
    private val onPhotoClick: (PhotoEntity, Int) -> Unit,
    private val onPhotoLongClick: (PhotoEntity) -> Unit = {}
) : ListAdapter<PhotoEntity, PhotosAdapter.PhotoViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class PhotoViewHolder(private val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: PhotoEntity, position: Int) {
            binding.ivPhoto.load(photo.uri) {
                crossfade(true)
                placeholder(com.family4.app.R.color.bg_elevated)
                error(com.family4.app.R.color.bg_elevated)
            }
            binding.root.setOnClickListener { onPhotoClick(photo, position) }
            binding.root.setOnLongClickListener { onPhotoLongClick(photo); true }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<PhotoEntity>() {
        override fun areItemsTheSame(o: PhotoEntity, n: PhotoEntity) = o.id == n.id
        override fun areContentsTheSame(o: PhotoEntity, n: PhotoEntity) = o == n
    }
}
