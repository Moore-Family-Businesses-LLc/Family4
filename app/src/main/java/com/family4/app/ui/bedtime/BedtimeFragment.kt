package com.family4.app.ui.bedtime

import android.os.Bundle
import android.view.*
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.family4.app.R
import com.family4.app.databinding.FragmentBedtimeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BedtimeFragment : Fragment() {

    private var _binding: FragmentBedtimeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BedtimeViewModel by viewModels()
    private lateinit var adapter: BedtimeAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentBedtimeBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BedtimeAdapter(
            onEdit   = { row -> showEditDialog(row) },
            onToggle = { alert -> viewModel.toggleAlert(alert) },
            onDelete = { alert -> viewModel.deleteAlert(alert) }
        )
        binding.rvBedtimes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBedtimes.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bedtimeRows.collectLatest { rows ->
                adapter.submitList(rows)
            }
        }
    }

    private fun showEditDialog(row: BedtimeRow) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_bedtime, null)
        val tpBed  = dialogView.findViewById<TimePicker>(R.id.tpBedtime)
        val tpWake = dialogView.findViewById<TimePicker>(R.id.tpWakeTime)
        tpBed.setIs24HourView(false)
        tpWake.setIs24HourView(false)

        row.alert?.let { a ->
            tpBed.hour   = a.bedtimeHour;   tpBed.minute   = a.bedtimeMinute
            tpWake.hour  = a.wakeHour;      tpWake.minute  = a.wakeMinute
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set bedtime for ${row.member.displayName}")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                viewModel.setBedtime(
                    row.member.id,
                    tpBed.hour, tpBed.minute,
                    tpWake.hour, tpWake.minute
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
