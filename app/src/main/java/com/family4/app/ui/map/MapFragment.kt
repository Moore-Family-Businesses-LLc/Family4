package com.family4.app.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Geocoder
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.family4.app.R
import com.family4.app.data.db.entity.FamilyMemberEntity
import com.family4.app.data.db.entity.SafeZoneEntity
import com.family4.app.databinding.FragmentMapBinding
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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

    // Markers keyed by memberId
    private val memberMarkers  = mutableMapOf<String, Marker>()
    // Polylines showing trail per member
    private val memberPolylines = mutableMapOf<String, Polyline>()
    // Circles for safe zones
    private val zoneCircles    = mutableMapOf<Long, Circle>()

    // Map type cycle: Normal → Satellite → Hybrid → Terrain
    private val mapTypes      = listOf(GoogleMap.MAP_TYPE_NORMAL, GoogleMap.MAP_TYPE_SATELLITE,
                                       GoogleMap.MAP_TYPE_HYBRID, GoogleMap.MAP_TYPE_TERRAIN)
    private val mapTypeLabels = listOf("Normal","Satellite","Hybrid","Terrain")
    private var currentMapTypeIndex = 0

    // Mode: FOLLOW (camera tracks selected member) vs FREE
    private var followMode = false
    private var trailsVisible = true

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
        setupToolbarActions()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.apply {
            isMyLocationButtonEnabled = false
            isZoomControlsEnabled     = false
            isCompassEnabled          = true
            isMapToolbarEnabled       = false
            isRotateGesturesEnabled   = true
            isTiltGesturesEnabled     = true
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) map.isMyLocationEnabled = true

        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        applyDarkStyle()
        observeAll()

        map.setOnMapClickListener { binding.memberInfoCard.isVisible = false }
        map.setOnMapLongClickListener { latLng -> promptAddSafeZone(latLng) }
        map.setOnMarkerClickListener { marker ->
            val memberId = marker.tag as? String ?: return@setOnMarkerClickListener false
            viewModel.familyLocations.value.firstOrNull { it.id == memberId }?.let {
                showMemberInfo(it); viewModel.selectMember(it)
            }
            true
        }
    }

    // ── Observe all data flows ────────────────────────────────────────────────
    private fun observeAll() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.familyLocations.collectLatest { members ->
                updateChips(members)
                updateMarkers(members)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.memberTrails.collectLatest { trails ->
                if (trailsVisible) updateTrails(trails) else clearTrails()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.safeZones.collectLatest { zones -> updateZones(zones) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedMember.collectLatest { member ->
                if (followMode && member != null) {
                    member.latitude?.let { lat ->
                        member.longitude?.let { lng ->
                            googleMap?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 16f))
                        }
                    }
                }
            }
        }
    }

    // ── Member chips ─────────────────────────────────────────────────────────
    private fun updateChips(members: List<FamilyMemberEntity>) {
        binding.chipGroupMembers.removeAllViews()
        members.forEach { member ->
            val chip = Chip(requireContext()).apply {
                text = member.displayName
                isCheckable = false
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    if (member.isOnline) 0xFF00D4FF.toInt() else 0xFF2A2A4A.toInt()
                )
                setTextColor(if (member.isOnline) 0xFF0D1B2A.toInt() else 0xFFBBBBBB.toInt())
                setOnClickListener {
                    viewModel.selectMember(member)
                    showMemberInfo(member)
                }
            }
            binding.chipGroupMembers.addView(chip)
        }
    }

    // ── Markers with custom avatar bitmaps ───────────────────────────────────
    private fun updateMarkers(members: List<FamilyMemberEntity>) {
        members.forEach { member ->
            val lat = member.latitude ?: return@forEach
            val lng = member.longitude ?: return@forEach
            val pos = LatLng(lat, lng)
            val existing = memberMarkers[member.id]
            if (existing != null) {
                existing.position = pos
                existing.snippet  = member.statusMessage
                existing.setIcon(createAvatarMarker(member))
            } else {
                val marker = googleMap?.addMarker(
                    MarkerOptions()
                        .position(pos)
                        .title(member.displayName)
                        .snippet(member.statusMessage.ifBlank { member.role })
                        .icon(createAvatarMarker(member))
                        .anchor(0.5f, 1f)
                )
                marker?.tag = member.id
                marker?.let { memberMarkers[member.id] = it }
            }
        }
        // Auto-fit when more than one has location
        val located = members.filter { it.latitude != null && it.longitude != null }
        if (located.size > 1 && !followMode) {
            val bounds = LatLngBounds.Builder()
            located.forEach { bounds.include(LatLng(it.latitude!!, it.longitude!!)) }
            try { googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 140)) }
            catch (_: Exception) {}
        }
    }

    /** Creates a 64×80dp bitmap: circle avatar with initials + coloured border. */
    private fun createAvatarMarker(member: FamilyMemberEntity): BitmapDescriptor {
        val size = (resources.displayMetrics.density * 56).toInt()
        val bmp  = Bitmap.createBitmap(size, size + 16, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Avatar colour — deterministic from id hash
        val colors = listOf(0xFF005C7A,0xFF3D1A6E,0xFF7A4200,0xFF1A6E3D,0xFF6E1A1A,
                            0xFF1A3D6E,0xFF5A1A6E,0xFF006E4D)
        val hue = colors[Math.abs(member.id.hashCode()) % colors.size].toInt()
        paint.color = hue or 0xFF000000.toInt()
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

        // Online ring
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = if (member.isOnline) 0xFF00D4FF.toInt() else 0xFF555555.toInt()
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, paint)

        // Initials text
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = size * 0.35f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        val initials = member.displayName.split(" ").take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
        val fm = paint.fontMetrics
        canvas.drawText(initials, size / 2f, size / 2f - (fm.ascent + fm.descent) / 2, paint)

        // Pointer triangle
        paint.style = Paint.Style.FILL
        paint.color = hue or 0xFF000000.toInt()
        val path = Path()
        path.moveTo(size * 0.4f, size.toFloat())
        path.lineTo(size * 0.6f, size.toFloat())
        path.lineTo(size / 2f, (size + 14).toFloat())
        path.close()
        canvas.drawPath(path, paint)

        return BitmapDescriptorFactory.fromBitmap(bmp)
    }

    // ── Trails (route history polylines) ─────────────────────────────────────
    private fun updateTrails(trails: List<MemberTrail>) {
        // Remove old polylines for members no longer in trails
        val currentIds = trails.map { it.memberId }.toSet()
        memberPolylines.keys.minus(currentIds).forEach { id ->
            memberPolylines.remove(id)?.remove()
        }
        trails.forEach { trail ->
            if (trail.points.size < 2) return@forEach
            val poly = memberPolylines[trail.memberId]
            if (poly != null) {
                poly.points = trail.points
            } else {
                val color = if (trail.isOnline) 0xAA00D4FF.toInt() else 0x558892B0.toInt()
                val newPoly = googleMap?.addPolyline(
                    PolylineOptions()
                        .addAll(trail.points)
                        .color(color)
                        .width(6f)
                        .geodesic(true)
                        .pattern(listOf(Dash(20f), Gap(10f)))
                ) ?: return@forEach
                memberPolylines[trail.memberId] = newPoly
            }
        }
    }

    private fun clearTrails() {
        memberPolylines.values.forEach { it.remove() }
        memberPolylines.clear()
    }

    // ── Safe zones ────────────────────────────────────────────────────────────
    private fun updateZones(zones: List<SafeZoneEntity>) {
        val currentIds = zones.map { it.id }.toSet()
        zoneCircles.keys.minus(currentIds).forEach { id -> zoneCircles.remove(id)?.remove() }
        zones.forEach { zone ->
            val existing = zoneCircles[zone.id]
            if (existing != null) {
                existing.center = LatLng(zone.latitude, zone.longitude)
                existing.radius = zone.radiusMeters.toDouble()
            } else {
                val circle = googleMap?.addCircle(
                    CircleOptions()
                        .center(LatLng(zone.latitude, zone.longitude))
                        .radius(zone.radiusMeters.toDouble())
                        .strokeColor(zone.color)
                        .fillColor(zone.color and 0x00FFFFFF or 0x22000000)
                        .strokeWidth(3f)
                ) ?: return@forEach
                zoneCircles[zone.id] = circle
            }
        }
    }

    private fun promptAddSafeZone(latLng: LatLng) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_safe_zone, null)
        val etName   = dialogView.findViewById<EditText>(R.id.etZoneName)
        val etRadius = dialogView.findViewById<EditText>(R.id.etZoneRadius)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Safe Zone")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = etName.text.toString().trim().ifEmpty { "Safe Zone" }
                val radius = etRadius.text.toString().toFloatOrNull() ?: 200f
                viewModel.addSafeZone(name, latLng.latitude, latLng.longitude, radius)
                Snackbar.make(binding.root, "Safe zone \"$name\" added", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Member info card ──────────────────────────────────────────────────────
    private fun showMemberInfo(member: FamilyMemberEntity) {
        binding.tvMemberName.text   = member.displayName
        binding.tvMemberStatus.text = member.statusMessage.ifBlank { member.role }
        binding.tvMemberBattery.text = when {
            member.batteryLevel < 0  -> ""
            member.batteryLevel <= 15 -> "🪫 ${member.batteryLevel}%"
            else                      -> "🔋 ${member.batteryLevel}%"
        }
        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        binding.tvMemberLastSeen.text = if (member.isOnline) "🟢 Online"
        else "Last seen ${timeFmt.format(Date(member.lastSeen))}"

        // Speed + ETA display
        val trail = viewModel.memberTrails.value.firstOrNull { it.memberId == member.id }
        val self  = viewModel.selfLocation.value
        if (trail != null && trail.speed > 1f) {
            val speedTxt = "%.0f km/h".format(trail.speed)
            val eta = if (self != null && member.latitude != null && member.longitude != null) {
                val dist = viewModel.haversineMeters(
                    self.latitude, self.longitude, member.latitude, member.longitude)
                viewModel.etaMinutes(dist, trail.speed)?.let { " · ETA ${it}min" } ?: ""
            } else ""
            binding.tvMemberSpeed.text = "🚀 $speedTxt$eta"
            binding.tvMemberSpeed.isVisible = true
        } else {
            binding.tvMemberSpeed.isVisible = false
        }

        binding.memberInfoCard.isVisible = true
        member.latitude?.let { lat ->
            member.longitude?.let { lng ->
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 15f))
            }
        }
    }

    // ── Toolbar actions ───────────────────────────────────────────────────────
    private fun setupToolbarActions() {
        binding.btnToggleTrails.setOnClickListener {
            trailsVisible = !trailsVisible
            if (trailsVisible) {
                updateTrails(viewModel.memberTrails.value)
                binding.btnToggleTrails.alpha = 1f
            } else {
                clearTrails()
                binding.btnToggleTrails.alpha = 0.4f
            }
        }
        binding.btnToggleFollow.setOnClickListener {
            followMode = !followMode
            binding.btnToggleFollow.alpha = if (followMode) 1f else 0.4f
            if (followMode) {
                Snackbar.make(binding.root, "Follow mode ON — tap a member chip",
                    Snackbar.LENGTH_SHORT).show()
            }
        }
        binding.btnManageZones.setOnClickListener { showZonesList() }
    }

    private fun showZonesList() {
        val zones = viewModel.safeZones.value
        if (zones.isEmpty()) {
            Snackbar.make(binding.root, "No safe zones yet. Long-press the map to add one.",
                Snackbar.LENGTH_LONG).show()
            return
        }
        val names = zones.map { "${it.name} (${it.radiusMeters.toInt()}m)" }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Safe Zones")
            .setItems(names) { _, idx ->
                val zone = zones[idx]
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(zone.latitude, zone.longitude), 15f))
            }
            .setNegativeButton("Delete…") { _, _ ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete a zone")
                    .setItems(names) { _, idx -> viewModel.deleteSafeZone(zones[idx]) }
                    .setNegativeButton("Cancel", null).show()
            }
            .setNeutralButton("Close", null)
            .show()
    }

    // ── Search ────────────────────────────────────────────────────────────────
    private fun setupSearch() {
        binding.etMapSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchAddress(binding.etMapSearch.text?.toString() ?: "")
                hideKeyboard(); true
            } else false
        }
    }

    @Suppress("DEPRECATION")
    private fun searchAddress(query: String) {
        if (query.isBlank()) return
        try {
            val results = Geocoder(requireContext(), Locale.getDefault())
                .getFromLocationName(query, 1)
            if (!results.isNullOrEmpty()) {
                val loc = results[0]
                val pos = LatLng(loc.latitude, loc.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 14f))
                googleMap?.addMarker(
                    MarkerOptions().position(pos).title(query)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                )
            } else {
                Snackbar.make(binding.root, "Address not found", Snackbar.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            Snackbar.make(binding.root, "Search failed", Snackbar.LENGTH_SHORT).show()
        }
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
            if (newType == GoogleMap.MAP_TYPE_NORMAL || newType == GoogleMap.MAP_TYPE_TERRAIN)
                applyDarkStyle()
            binding.ivMapType.contentDescription = mapTypeLabels[currentMapTypeIndex]
        }
    }

    // ── FABs ──────────────────────────────────────────────────────────────────
    private fun setupFabs() {
        binding.fabZoomIn.setOnClickListener   { googleMap?.animateCamera(CameraUpdateFactory.zoomIn()) }
        binding.fabZoomOut.setOnClickListener  { googleMap?.animateCamera(CameraUpdateFactory.zoomOut()) }
        binding.fabMyLocation.setOnClickListener {
            viewModel.selfLocation.value?.let {
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f))
            }
        }
    }

    // ── Dark style ────────────────────────────────────────────────────────────
    private fun applyDarkStyle() {
        val style = """[
          {"elementType":"geometry","stylers":[{"color":"#0d1b2a"}]},
          {"elementType":"labels.icon","stylers":[{"visibility":"off"}]},
          {"elementType":"labels.text.fill","stylers":[{"color":"#00d4ff"}]},
          {"elementType":"labels.text.stroke","stylers":[{"color":"#0d1b2a"}]},
          {"featureType":"road","elementType":"geometry.fill","stylers":[{"color":"#1a3a5c"}]},
          {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#00536e"}]},
          {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0b1e33"}]},
          {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#0f2a1a"}]},
          {"featureType":"transit","elementType":"labels.text.fill","stylers":[{"color":"#757575"}]}
        ]"""
        try { googleMap?.setMapStyle(MapStyleOptions(style)) } catch (_: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
