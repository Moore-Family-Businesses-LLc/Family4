package com.family4.app.ui.notes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.family4.app.R
import com.family4.app.data.db.entity.NoteEntity
import com.family4.app.databinding.ItemNoteCardBinding
import java.text.SimpleDateFormat
import java.util.*

class NotesAdapter(
    private val onNoteClick: (NoteEntity) -> Unit,
    private val onNoteLongClick: (NoteEntity) -> Unit,
    private val onNotePin: (NoteEntity) -> Unit = {},
    private val onNoteDelete: (NoteEntity) -> Unit = {},
    private val onNoteArchive: (NoteEntity) -> Unit = {}
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
            binding.tvNoteTitle.text = note.title.ifBlank { "" }
            // Hide title row if blank — Google Keep hides it
            binding.tvNoteTitle.visibility =
                if (note.title.isBlank()) View.GONE else View.VISIBLE

            binding.tvNoteContent.text = note.content
            binding.tvNoteContent.visibility =
                if (note.content.isBlank()) View.GONE else View.VISIBLE

            binding.tvNoteDate.text = SimpleDateFormat("MMM d", Locale.getDefault())
                .format(Date(note.updatedAt))

            // Dark-mode note card background
            val noteColor = if (note.color != 0) note.color
                else binding.root.context.getColor(R.color.note_default)
            binding.cardNote.setCardBackgroundColor(noteColor)

            // Accent strip — use note color if set, otherwise default cyan
            val stripColor = if (note.color != 0) note.color
                else binding.root.context.getColor(R.color.accent_cyan)
            binding.noteColorStrip.setBackgroundColor(stripColor)

            // Pin icon
            binding.ivPin.visibility = if (note.isPinned) View.VISIBLE else View.GONE

            // Checklist icon
            binding.ivChecklist.visibility =
                if (note.isChecklist) View.VISIBLE else View.GONE

            // 3-dot overflow popup
            binding.btnNoteOverflow.setOnClickListener { anchor ->
                val popup = PopupMenu(anchor.context, anchor)
                popup.menuInflater.inflate(R.menu.menu_note_card, popup.menu)
                // Update pin label dynamically
                popup.menu.findItem(R.id.action_note_pin)?.title =
                    if (note.isPinned) "Unpin" else "Pin"
                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_note_pin    -> { onNotePin(note); true }
                        R.id.action_note_archive -> { onNoteArchive(note); true }
                        R.id.action_note_delete -> { onNoteDelete(note); true }
                        else -> false
                    }
                }
                popup.show()
            }

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
