package com.family4.app.ui.notes

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.data.db.entity.NoteEntity
import com.family4.app.databinding.ItemNoteCardBinding
import java.text.SimpleDateFormat
import java.util.*

class NotesAdapter(
    private val onNoteClick: (NoteEntity) -> Unit,
    private val onNoteLongClick: (NoteEntity) -> Unit
) : ListAdapter<NoteEntity, NotesAdapter.NoteViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(private val binding: ItemNoteCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: NoteEntity) {
            binding.tvNoteTitle.text = note.title.ifBlank { "Untitled" }
            binding.tvNoteContent.text = note.content
            binding.tvNoteDate.text = SimpleDateFormat("MMM d", Locale.getDefault())
                .format(Date(note.updatedAt))
            binding.cardNote.setCardBackgroundColor(
                if (note.color != 0xFFFFFFFF.toInt()) note.color else Color.WHITE
            )
            binding.ivPin.visibility =
                if (note.isPinned) android.view.View.VISIBLE else android.view.View.GONE
            binding.ivChecklist.visibility =
                if (note.isChecklist) android.view.View.VISIBLE else android.view.View.GONE

            binding.root.setOnClickListener { onNoteClick(note) }
            binding.root.setOnLongClickListener {
                onNoteLongClick(note)
                true
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<NoteEntity>() {
        override fun areItemsTheSame(o: NoteEntity, n: NoteEntity) = o.id == n.id
        override fun areContentsTheSame(o: NoteEntity, n: NoteEntity) = o == n
    }
}
