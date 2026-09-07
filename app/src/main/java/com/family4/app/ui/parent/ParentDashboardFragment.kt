package com.family4.app.ui.parent

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.family4.app.R
import com.family4.app.databinding.FragmentParentDashboardBinding
import com.family4.app.security.StealthModeManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ParentDashboardFragment : Fragment() {

    private var _binding: FragmentParentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ParentDashboardViewModel by viewModels()
    private lateinit var adapter: KidSummaryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?
    ): View {
        _binding = FragmentParentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = KidSummaryAdapter(
            onSetLimit = { kid, limit -> viewModel.setScreenTimeLimit(kid.member.id, limit) }
        )
        binding.rvKids.layoutManager = LinearLayoutManager(requireContext())
        binding.rvKids.adapter = adapter

        // Stealth mode toggle
        binding.switchStealth.isChecked = StealthModeManager.isStealthEnabled(requireContext())
        binding.switchStealth.setOnCheckedChangeListener { _, checked ->
            viewLifecycleOwner.lifecycleScope.launch {
                StealthModeManager.setStealthEnabled(requireContext(), checked)
                binding.btnSetPattern.isEnabled = checked
                if (checked && StealthModeManager.isPatternExpired(requireContext())) {
                    binding.tvPatternWarning.visibility = View.VISIBLE
                }
            }
        }

        binding.btnSetPattern.setOnClickListener {
            PatternSetupBottomSheet().show(parentFragmentManager, "pattern")
        }

        binding.tvPatternWarning.visibility =
            if (StealthModeManager.isStealthEnabled(requireContext()) &&
                StealthModeManager.isPatternExpired(requireContext()))
                View.VISIBLE else View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.kidSummaries.collectLatest { summaries ->
                adapter.submitList(summaries)
                binding.tvNoKids.visibility =
                    if (summaries.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
