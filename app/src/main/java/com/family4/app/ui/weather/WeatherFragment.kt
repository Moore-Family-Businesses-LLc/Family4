package com.family4.app.ui.weather

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.family4.app.databinding.FragmentWeatherBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRefreshWeather.setOnClickListener { viewModel.refresh() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.weather.collectLatest { data ->
                if (data != null) {
                    binding.tvCityName.text       = data.cityName
                    binding.tvTemperature.text    = "${data.tempC.toInt()}°C"
                    binding.tvFeelsLike.text      = "Feels like ${data.feelsLikeC.toInt()}°"
                    binding.tvDescription.text    = data.description.replaceFirstChar { it.uppercaseChar() }
                    binding.tvHumidity.text       = "💧 ${data.humidity}%"
                    binding.tvWindSpeed.text      = "💨 ${data.windKph} km/h"
                    binding.tvWeatherIcon.text    = data.emoji
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { loading ->
                binding.progressWeather.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { err ->
                binding.tvError.text = err ?: ""
                binding.tvError.visibility = if (err != null) android.view.View.VISIBLE else android.view.View.GONE
            }
        }

        viewModel.refresh()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
