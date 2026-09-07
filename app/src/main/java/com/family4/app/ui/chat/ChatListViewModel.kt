package com.family4.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.FamilyMemberDao
import com.family4.app.data.db.entity.FamilyMemberEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val memberDao: FamilyMemberDao
) : ViewModel() {

    val members: StateFlow<List<FamilyMemberEntity>> = memberDao.getAllMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Insert demo members the very first time the DB is empty so the screen isn't blank. */
    fun seedDemoMembersIfEmpty() = viewModelScope.launch {
        val current = members.value
        if (current.isNotEmpty()) return@launch
        listOf(
            FamilyMemberEntity(
                id = "demo_self",
                displayName = "Me (Owner)",
                role = "ADMIN",
                isOnline = true,
                statusMessage = "Family4 Admin",
                batteryLevel = 92
            ),
            FamilyMemberEntity(
                id = "demo_spouse",
                displayName = "Spouse",
                role = "ADMIN",
                isOnline = true,
                statusMessage = "At work",
                batteryLevel = 61
            ),
            FamilyMemberEntity(
                id = "demo_child1",
                displayName = "Child 1",
                role = "CHILD",
                isOnline = false,
                statusMessage = "At school",
                batteryLevel = 44
            ),
            FamilyMemberEntity(
                id = "demo_child2",
                displayName = "Child 2",
                role = "CHILD",
                isOnline = false,
                statusMessage = "",
                batteryLevel = 78
            )
        ).forEach { memberDao.insertMember(it) }
    }

    fun addMember(name: String, phone: String, role: String) = viewModelScope.launch {
        memberDao.insertMember(
            FamilyMemberEntity(
                id = UUID.randomUUID().toString(),
                displayName = name,
                phoneNumber = phone.ifBlank { null },
                role = role.ifBlank { "MEMBER" }
            )
        )
    }

    fun removeMember(member: FamilyMemberEntity) = viewModelScope.launch {
        memberDao.deleteMember(member)
    }
}
