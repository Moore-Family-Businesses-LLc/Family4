package com.family4.app.ui.notes

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.family4.app.R
import com.family4.app.databinding.FragmentNoteDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class NoteDetailFragment : Fragment() {

    private var _binding: FragmentNoteDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NoteDetailViewModel by viewModels()
    private val args: NoteDetailFragmentArgs by navArgs()

    private var selectedColor: Int = NoteDetailViewModel.DEFAULT_NOTE_COLOR
    private var isPinned: Boolean = false
    private var colorPanelVisible = false
    private val swatches = mutableListOf<View>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?
    ): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buildColorPicker()
        applyNoteBackground(selectedColor)

        if (args.noteId != -1L) {
            viewModel.loadNote(args.noteId)
        }

        viewModel.note.observe(viewLifecycleOwner) { note ->
            note ?: return@observe
            binding.etNoteTitle.setText(note.title)
            binding.etNoteContent.setText(note.content)
            isPinned = note.isPinned
            updatePinIcon()
            val noteColor = if (note.color != 0) note.color
                            else NoteDetailViewModel.DEFAULT_NOTE_COLOR
            applyNoteBackground(noteColor)

            // "Edited …" label
            val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            binding.tvNoteEdited.text = "Edited ${fmt.format(Date(note.updatedAt))}"
        }

        setupToolbar()
        setupBackPress()
    }

    // ── Bottom toolbar wiring ─────────────────────────────────────────────────

    private fun setupToolbar() {
        binding.btnNoteColorPalette.setOnClickListener {
            colorPanelVisible = !colorPanelVisible
            binding.colorPickerPanel.visibility =
                if (colorPanelVisible) View.VISIBLE else View.GONE
        }

        binding.btnNotePin.setOnClickListener {
            isPinned = !isPinned
            updatePinIcon()
        }

        binding.btnNoteDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage("Delete this note?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteNote()
                    findNavController().popBackStack()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnNoteAddItem.setOnClickListener {
            // Move cursor to end of content area
            binding.etNoteContent.requestFocus()
            binding.etNoteContent.setSelection(
                binding.etNoteContent.text?.length ?: 0
            )
        }
    }

    // ── Auto-save on back ─────────────────────────────────────────────────────

    private fun setupBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    saveNote()
                }
            }
        )
    }

    private fun saveNote() {
        val title   = binding.etNoteTitle.text?.toString() ?: ""
        val content = binding.etNoteContent.text?.toString() ?: ""
        if (title.isNotBlank() || content.isNotBlank()) {
            viewModel.saveNote(title, content, selectedColor, isPinned)
        }
        findNavController().popBackStack()
    }

    // ── Colour picker ─────────────────────────────────────────────────────────

    private fun buildColorPicker() {
        val context = requireContext()
        val density = resources.displayMetrics.density
        val size    = (40 * density).toInt()
        val margin  = (6 * density).toInt()

        binding.colorPickerRow.removeAllViews()
        swatches.clear()

        NOTE_COLORS.forEach { colorRes ->
            val color = ContextCompat.getColor(context, colorRes)
            val swatch = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = margin; marginEnd = margin
                }
                background = swatchDrawable(color, selected = false)
                contentDescription = getString(R.string.note_color_label)
                setOnClickListener {
                    applyNoteBackground(color)
                    // Don't auto-close; let user pick multiple or close manually
                }
            }
            swatches.add(swatch)
            binding.colorPickerRow.addView(swatch)
        }
    }

    private fun swatchDrawable(color: Int, selected: Boolean): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            val dp = resources.displayMetrics.density
            if (selected) setStroke((3 * dp).toInt(), ContextCompat.getColor(requireContext(), R.color.accent_cyan))
            else          setStroke((2 * dp).toInt(), Color.parseColor("#44FFFFFF"))
        }

    private fun applyNoteBackground(color: Int) {
        selectedColor = color
        binding.noteDetailRoot.setBackgroundColor(color)
        // Update swatch rings
        NOTE_COLORS.forEachIndexed { i, res ->
            val c = ContextCompat.getColor(requireContext(), res)
            swatches.getOrNull(i)?.background = swatchDrawable(c, c == color)
        }
    }

    private fun updatePinIcon() {
        binding.btnNotePin.setColorFilter(
            ContextCompat.getColor(
                requireContext(),
                if (isPinned) R.color.accent_cyan else R.color.text_muted
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        swatches.clear()
        _binding = null
    }

    private companion object {
        val NOTE_COLORS = intArrayOf(
            R.color.note_white,
            R.color.note_red,
            R.color.note_orange,
            R.color.note_yellow,
            R.color.note_green,
            R.color.note_teal,
            R.color.note_blue,
            R.color.note_purple,
            R.color.note_pink,
            R.color.note_gray
        )
    }
}
