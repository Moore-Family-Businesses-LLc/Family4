package com.family4.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.FamilyMemberDao
import com.family4.app.data.db.dao.LocationDao
import com.family4.app.data.db.entity.FamilyMemberEntity
import com.family4.app.data.db.entity.LocationSnapshotEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val memberDao: FamilyMemberDao,
    private val locationDao: LocationDao
) : ViewModel() {

    val familyLocations: StateFlow<List<FamilyMemberEntity>> = memberDao.getAllMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selfLocation = MutableStateFlow<LocationSnapshotEntity?>(null)
    val selfLocation: StateFlow<LocationSnapshotEntity?> = _selfLocation

    init {
        viewModelScope.launch {
            _selfLocation.value = locationDao.getLatestLocation("self")
        }
    }
}
