package com.family4.app.ui.sos

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.FamilyMemberDao
import com.family4.app.data.db.dao.LocationDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencySOSViewModel @Inject constructor(
    private val memberDao: FamilyMemberDao,
    private val locationDao: LocationDao
) : ViewModel() {

    fun triggerSOS() = viewModelScope.launch {
        // In a real implementation:
        // 1. Send push notification to all members with GPS coordinates
        // 2. POST to emergency endpoint using locationDao.getLatestLocation("self")
        // 3. Notify memberDao.getAllMembers()
        // 4. Optionally dial 911
        // For now we expose a state that the Fragment observes to launch the dialer
    }
}
