package com.family4.app.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.family4.app.R
import com.family4.app.data.db.entity.FamilyMemberEntity
import com.family4.app.databinding.FragmentMapBinding
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()
    private var googleMap: GoogleMap? = null
    private val memberMarkers = mutableMapOf<String, Marker>()

    // Map type cycles: Normal → Satellite → Hybrid → Terrain → Normal
    private val mapTypes = listOf(
        GoogleMap.MAP_TYPE_NORMAL,
        GoogleMap.MAP_TYPE_SATELLITE,
        GoogleMap.MAP_TYPE_HYBRID,
        GoogleMap.MAP_TYPE_TERRAIN
    )
    private val mapTypeLabels = listOf("Normal", "Satellite", "Hybrid", "Terrain")
    private var currentMapTypeIndex = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFrag = childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapFrag.getMapAsync(this)
        setupSearch()
        setupFabs()
        setupMapTypeToggle()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // UI settings
        map.uiSettings.apply {
            isMyLocationButtonEnabled  = false
            isZoomControlsEnabled      = false   // we use our own FABs
            isCompassEnabled           = true
            isMapToolbarEnabled        = false
            isRotateGesturesEnabled    = true
            isTiltGesturesEnabled      = true
            isScrollGesturesEnabled    = true
            isZoomGesturesEnabled      = true
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        }

        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        applyDarkStyle()
        observeMembers()

        // Tap on map closes member info card
        map.setOnMapClickListener {
            binding.memberInfoCard.isVisible = false
        }
    }

    // ── Rich dark map style ───────────────────────────────────────────────────
    private fun applyDarkStyle() {
        val styleJson = """
        [
          {"elementType":"geometry","stylers":[{"color":"#0d1b2a"}]},
          {"elementType":"labels.icon","stylers":[{"visibility":"off"}]},
          {"elementType":"labels.text.fill","stylers":[{"color":"#00d4ff"}]},
          {"elementType":"labels.text.stroke","stylers":[{"color":"#0d1b2a"}]},
          {"featureType":"administrative","elementType":"geometry","stylers":[{"color":"#1a2744"}]},
          {"featureType":"administrative.country","elementType":"labels.text.fill","stylers":[{"color":"#9aa0a6"}]},
          {"featureType":"administrative.locality","elementType":"labels.text.fill","stylers":[{"color":"#bdbdbd"}]},
          {"featureType":"poi","elementType":"labels.text.fill","stylers":[{"color":"#757575"}]},
          {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#0f2a1a"}]},
          {"featureType":"poi.park","elementType":"labels.text.fill","stylers":[{"color":"#616161"}]},
          {"featureType":"poi.park","elementType":"labels.text.stroke","stylers":[{"color":"#1b1b1b"}]},
          {"featureType":"road","elementType":"geometry.fill","stylers":[{"color":"#1a3a5c"}]},
          {"featureType":"road","elementType":"geometry.stroke","stylers":[{"color":"#0f2840"}]},
          {"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#8a8a8a"}]},
          {"featureType":"road.arterial","elementType":"geometry","stylers":[{"color":"#1f3d6e"}]},
          {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#00536e"}]},
          {"featureType":"road.highway.controlled_access","elementType":"geometry","stylers":[{"color":"#004a5e"}]},
          {"featureType":"road.local","elementType":"labels.text.fill","stylers":[{"color":"#616161"}]},
          {"featureType":"transit","elementType":"labels.text.fill","stylers":[{"color":"#757575"}]},
          {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0b1e33"}]},
          {"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#3d3d3d"}]}
        ]"""
        try {
            googleMap?.setMapStyle(MapStyleOptions(styleJson))
        } catch (_: Exception) {}
    }

    // ── Member chips + markers ────────────────────────────────────────────────
    private fun observeMembers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.familyLocations.collectLatest { members ->
                updateChips(members)
                updateMarkers(members)
            }
        }
    }

    private fun updateChips(members: List<FamilyMemberEntity>) {
        binding.chipGroupMembers.removeAllViews()
        members.forEach { member ->
            val chip = Chip(requireContext()).apply {
                text = member.displayName
                isCheckable = false
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    if (member.isOnline) 0xFF00D4FF.toInt() else 0xFF2A2A4A.toInt()
                )
                setTextColor(
                    if (member.isOnline) 0xFF0D1B2A.toInt() else 0xFFBBBBBB.toInt()
                )
                setOnClickListener { showMemberInfo(member) }
            }
            binding.chipGroupMembers.addView(chip)
        }
    }

    private fun updateMarkers(members: List<FamilyMemberEntity>) {
        members.forEach { member ->
            val lat = member.latitude ?: return@forEach
            val lng = member.longitude ?: return@forEach
            val pos = LatLng(lat, lng)
            val existing = memberMarkers[member.id]
            if (existing != null) {
                existing.position = pos
                existing.snippet = member.statusMessage
            } else {
                val hue = if (member.role == "ADMIN") BitmapDescriptorFactory.HUE_AZURE
                          else BitmapDescriptorFactory.HUE_VIOLET
                val marker = googleMap?.addMarker(
                    MarkerOptions()
                        .position(pos)
                        .title(member.displayName)
                        .snippet(member.statusMessage.ifBlank { member.role })
                        .icon(BitmapDescriptorFactory.defaultMarker(hue))
                )
                marker?.let { memberMarkers[member.id] = it }
            }
        }
        // Auto-fit bounds if more than one member has location
        val located = members.filter { it.latitude != null && it.longitude != null }
        if (located.size > 1) {
            val bounds = LatLngBounds.Builder()
            located.forEach { bounds.include(LatLng(it.latitude!!, it.longitude!!)) }
            try {
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 140))
            } catch (_: Exception) {}
        }
    }

    private fun showMemberInfo(member: FamilyMemberEntity) {
        binding.tvMemberName.text   = member.displayName
        binding.tvMemberStatus.text = member.statusMessage.ifBlank { member.role }
        binding.tvMemberBattery.text = if (member.batteryLevel >= 0) "🔋 ${member.batteryLevel}%" else ""
        val lastSeenFmt = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(member.lastSeen))
        binding.tvMemberLastSeen.text = if (member.isOnline) "🟢 Online" else "Last seen $lastSeenFmt"
        binding.memberInfoCard.isVisible = true

        // Fly to member if they have a location
        member.latitude?.let { lat ->
            member.longitude?.let { lng ->
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f))
            }
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────
    private fun setupSearch() {
        binding.etMapSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchAddress(binding.etMapSearch.text?.toString() ?: "")
                hideKeyboard()
                true
            } else false
        }
    }

    @Suppress("DEPRECATION")
    private fun searchAddress(query: String) {
        if (query.isBlank()) return
        try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val results = geocoder.getFromLocationName(query, 1)
            if (!results.isNullOrEmpty()) {
                val loc = results[0]
                val pos = LatLng(loc.latitude, loc.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f))
                googleMap?.addMarker(
                    MarkerOptions().position(pos).title(query)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                )
            }
        } catch (_: Exception) {}
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etMapSearch.windowToken, 0)
    }

    // ── Map type toggle ───────────────────────────────────────────────────────
    private fun setupMapTypeToggle() {
        binding.btnMapType.setOnClickListener {
            currentMapTypeIndex = (currentMapTypeIndex + 1) % mapTypes.size
            val newType = mapTypes[currentMapTypeIndex]
            googleMap?.mapType = newType
            // Re-apply dark style only on Normal/Terrain types
            if (newType == GoogleMap.MAP_TYPE_NORMAL || newType == GoogleMap.MAP_TYPE_TERRAIN) {
                applyDarkStyle()
            }
            binding.ivMapType.contentDescription = mapTypeLabels[currentMapTypeIndex]
        }
    }

    // ── FABs ──────────────────────────────────────────────────────────────────
    private fun setupFabs() {
        binding.fabZoomIn.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }
        binding.fabZoomOut.setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }
        binding.fabMyLocation.setOnClickListener {
            viewModel.selfLocation.value?.let { loc ->
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16f)
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
