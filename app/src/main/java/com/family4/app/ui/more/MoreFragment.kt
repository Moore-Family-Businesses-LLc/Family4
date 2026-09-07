package com.family4.app.ui.more

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.family4.app.R
import com.family4.app.databinding.FragmentMoreBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MoreFragment : Fragment() {

    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentMoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cardWalkie.setOnClickListener { findNavController().navigate(R.id.nav_walkie_talkie) }
        binding.cardSos.setOnClickListener { findNavController().navigate(R.id.nav_sos) }
        binding.cardHealth.setOnClickListener { findNavController().navigate(R.id.nav_health) }
        binding.cardAlbums.setOnClickListener { findNavController().navigate(R.id.nav_albums) }
        binding.cardWeather.setOnClickListener { findNavController().navigate(R.id.nav_weather) }
        binding.cardTasks.setOnClickListener { findNavController().navigate(R.id.nav_tasks) }
        binding.cardNotes.setOnClickListener { findNavController().navigate(R.id.nav_notes) }
        binding.cardCalendar.setOnClickListener { findNavController().navigate(R.id.nav_calendar) }
        binding.cardFiles.setOnClickListener { findNavController().navigate(R.id.nav_files) }
        binding.cardSettings.setOnClickListener { findNavController().navigate(R.id.nav_settings) }
        binding.cardAi.setOnClickListener { findNavController().navigate(R.id.nav_ai_assistant) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
