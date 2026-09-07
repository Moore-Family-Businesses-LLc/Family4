package com.family4.app.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.FamilyMemberDao
import com.family4.app.data.db.dao.LocationDao
import com.family4.app.data.db.dao.SafeZoneDao
import com.family4.app.data.db.entity.FamilyMemberEntity
import com.family4.app.data.db.entity.LocationSnapshotEntity
import com.family4.app.data.db.entity.SafeZoneEntity
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

data class MemberTrail(
    val memberId: String,
    val displayName: String,
    val points: List<LatLng>,
    val speed: Float,          // m/s from latest two points
    val isOnline: Boolean
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val app: Application,
    private val memberDao: FamilyMemberDao,
    private val locationDao: LocationDao,
    private val safeZoneDao: SafeZoneDao
) : AndroidViewModel(app) {

    val familyLocations: StateFlow<List<FamilyMemberEntity>> = memberDao.getAllMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selfLocation = MutableStateFlow<LocationSnapshotEntity?>(null)
    val selfLocation: StateFlow<LocationSnapshotEntity?> = _selfLocation

    val safeZones: StateFlow<List<SafeZoneEntity>> = safeZoneDao.getAllZones()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _memberTrails = MutableStateFlow<List<MemberTrail>>(emptyList())
    val memberTrails: StateFlow<List<MemberTrail>> = _memberTrails

    private val _selectedMember = MutableStateFlow<FamilyMemberEntity?>(null)
    val selectedMember: StateFlow<FamilyMemberEntity?> = _selectedMember

    init {
        viewModelScope.launch {
            _selfLocation.value = locationDao.getLatestLocation("self")
        }
        viewModelScope.launch {
            memberDao.getAllMembers().collectLatest { members ->
                val trails = members.mapNotNull { member ->
                    val history = locationDao.getLocationHistory(member.id, 20).first()
                    if (history.isEmpty()) return@mapNotNull null
                    val points = history.map { LatLng(it.latitude, it.longitude) }
                    val speed = calculateSpeed(history)
                    MemberTrail(member.id, member.displayName, points, speed, member.isOnline)
                }
                _memberTrails.value = trails
            }
        }
    }

    fun selectMember(member: FamilyMemberEntity?) {
        _selectedMember.value = member
    }

    fun addSafeZone(name: String, lat: Double, lng: Double, radiusM: Float) {
        viewModelScope.launch {
            safeZoneDao.insert(SafeZoneEntity(
                name = name, latitude = lat, longitude = lng,
                radiusMeters = radiusM
            ))
        }
    }

    fun deleteSafeZone(zone: SafeZoneEntity) {
        viewModelScope.launch { safeZoneDao.delete(zone) }
    }

    /** Returns speed in km/h from the two most recent location points. */
    private fun calculateSpeed(history: List<LocationSnapshotEntity>): Float {
        if (history.size < 2) return history.firstOrNull()?.speed?.let { it * 3.6f } ?: 0f
        val a = history[0]; val b = history[1]
        val dtSec = (a.timestamp - b.timestamp) / 1000f
        if (dtSec <= 0f) return 0f
        val distM = haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)
        return (distM.toFloat() / dtSec) * 3.6f  // m/s → km/h
    }

    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6_371_000.0
        val phi1 = Math.toRadians(lat1); val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLam = Math.toRadians(lon2 - lon1)
        val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLam / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /** Straight-line ETA in minutes given current speed in km/h and distance in meters. */
    fun etaMinutes(distMeters: Double, speedKmh: Float): Int? {
        if (speedKmh < 1f) return null
        val hours = (distMeters / 1000.0) / speedKmh
        return (hours * 60).toInt().coerceAtLeast(1)
    }
}
