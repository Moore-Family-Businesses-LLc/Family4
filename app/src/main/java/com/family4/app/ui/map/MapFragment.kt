package com.family4.app.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.family4.app.R
import com.family4.app.databinding.FragmentMapBinding
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()
    private var googleMap: GoogleMap? = null
    private val memberMarkers = mutableMapOf<String, Marker>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapView) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setupFab()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isMyLocationButtonEnabled = false
        map.uiSettings.isZoomControlsEnabled = false
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        }

        observeMembers()
        setupMapStyle()
    }

    private fun setupMapStyle() {
        // Dark map style applied via JSON string to avoid R.raw dependency
        try {
            val styleJson = """[{"elementType":"geometry","stylers":[{"color":"#1a1a2e"}]},
                {"elementType":"labels.text.fill","stylers":[{"color":"#00d4ff"}]},
                {"featureType":"road","elementType":"geometry","stylers":[{"color":"#0f3460"}]},
                {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0f3460"}]}]"""
            googleMap?.setMapStyle(MapStyleOptions(styleJson))
        } catch (_: Exception) { /* Use default style if parsing fails */ }
    }

    private fun observeMembers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.familyLocations.collectLatest { members ->
                members.forEach { member ->
                    val lat = member.latitude ?: return@forEach
                    val lng = member.longitude ?: return@forEach
                    val pos = LatLng(lat, lng)
                    val existing = memberMarkers[member.id]
                    if (existing != null) {
                        existing.position = pos
                        existing.title = member.displayName
                    } else {
                        val marker = googleMap?.addMarker(
                            MarkerOptions()
                                .position(pos)
                                .title(member.displayName)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        )
                        marker?.let { memberMarkers[member.id] = it }
                    }
                }
                // Center on family
                if (members.isNotEmpty()) {
                    val bounds = LatLngBounds.Builder()
                    members.forEach { m ->
                        m.latitude?.let { lat ->
                            m.longitude?.let { lng -> bounds.include(LatLng(lat, lng)) }
                        }
                    }
                    try {
                        googleMap?.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(bounds.build(), 120)
                        )
                    } catch (_: Exception) { }
                }
            }
        }
    }

    private fun setupFab() {
        binding.fabMyLocation.setOnClickListener {
            // Re-center on user's last known location
            viewModel.selfLocation.value?.let { loc ->
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f)
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
