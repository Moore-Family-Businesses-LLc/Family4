package com.family4.app.ui.polls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.FamilyMemberDao
import com.family4.app.data.db.dao.PollDao
import com.family4.app.data.db.entity.PollEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class FamilyPollViewModel @Inject constructor(
    private val pollDao: PollDao,
    private val memberDao: FamilyMemberDao
) : ViewModel() {

    val activePolls: StateFlow<List<PollEntity>> = pollDao.getActivePolls()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createPoll(question: String, options: List<String>, createdBy: String) {
        viewModelScope.launch {
            val optArr = JSONArray(options).toString()
            pollDao.insert(PollEntity(question = question, optionsJson = optArr, createdBy = createdBy))
        }
    }

    fun vote(poll: PollEntity, option: String, memberId: String) {
        viewModelScope.launch {
            try {
                val votes = JSONObject(poll.votesJson)
                // Remove this member from any existing vote
                val keys = votes.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val arr = JSONArray(votes.getString(key))
                    val filtered = (0 until arr.length())
                        .map { arr.getString(it) }
                        .filter { it != memberId }
                    votes.put(key, JSONArray(filtered))
                }
                // Add vote to chosen option
                val arr = if (votes.has(option)) JSONArray(votes.getString(option)) else JSONArray()
                arr.put(memberId)
                votes.put(option, arr)
                pollDao.update(poll.copy(votesJson = votes.toString()))
            } catch (_: Exception) {}
        }
    }

    fun closePoll(poll: PollEntity) {
        viewModelScope.launch { pollDao.update(poll.copy(isActive = false)) }
    }

    fun deletePoll(poll: PollEntity) {
        viewModelScope.launch { pollDao.delete(poll) }
    }
}
