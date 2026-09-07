package com.family4.app.ui.files

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.R
import com.family4.app.databinding.ItemFileBinding
import java.text.SimpleDateFormat
import java.util.*

class FilesAdapter : ListAdapter<DriveFile, FilesAdapter.VH>(DIFF) {

    var onDeleteClick: ((DriveFile) -> Unit)? = null
    var onShareClick:  ((DriveFile) -> Unit)? = null

    inner class VH(private val b: ItemFileBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(file: DriveFile) {
            b.tvFileName.text  = file.name
            b.tvFileSize.text  = formatSize(file.size)
            b.tvFileDate.text  = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                .format(Date(file.modifiedAt))
            b.ivFileIcon.setImageResource(iconForMime(file.mimeType))
            b.btnDelete.setOnClickListener { onDeleteClick?.invoke(file) }
            b.btnShare.setOnClickListener  { onShareClick?.invoke(file) }
        }

        private fun formatSize(bytes: Long): String = when {
            bytes < 1_024           -> "${bytes}B"
            bytes < 1_048_576       -> "${bytes / 1_024}KB"
            bytes < 1_073_741_824   -> "${"%.1f".format(bytes / 1_048_576f)}MB"
            else                    -> "${"%.1f".format(bytes / 1_073_741_824f)}GB"
        }

        private fun iconForMime(mime: String): Int = when {
            mime.startsWith("image/") -> R.drawable.ic_nav_albums
            mime.startsWith("video/") -> R.drawable.ic_nav_camera
            mime.contains("pdf")      -> R.drawable.ic_nav_notes
            else                      -> R.drawable.ic_upload
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<DriveFile>() {
            override fun areItemsTheSame(a: DriveFile, b: DriveFile) = a.id == b.id
            override fun areContentsTheSame(a: DriveFile, b: DriveFile) = a == b
        }
    }
}
