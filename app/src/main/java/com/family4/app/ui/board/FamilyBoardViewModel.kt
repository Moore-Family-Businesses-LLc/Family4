package com.family4.app.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.BoardDao
import com.family4.app.data.db.entity.BoardPostEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class FamilyBoardViewModel @Inject constructor(
    private val boardDao: BoardDao
) : ViewModel() {

    val posts = boardDao.getAllPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addPost(content: String, authorId: String = "self", authorName: String = "Me") {
        if (content.isBlank()) return
        viewModelScope.launch {
            boardDao.insertPost(
                BoardPostEntity(
                    authorId = authorId,
                    authorName = authorName,
                    content = content.trim()
                )
            )
        }
    }

    fun deletePost(post: BoardPostEntity) = viewModelScope.launch {
        boardDao.deletePost(post)
    }

    fun togglePin(post: BoardPostEntity) = viewModelScope.launch {
        boardDao.setPinned(post.id, !post.pinned)
    }

    fun react(post: BoardPostEntity, emoji: String) = viewModelScope.launch {
        val current = runCatching {
            if (post.reactions.isBlank()) JSONObject()
            else JSONObject(post.reactions)
        }.getOrDefault(JSONObject())
        val count = current.optInt(emoji, 0) + 1
        current.put(emoji, count)
        boardDao.updateReactions(post.id, current.toString())
    }

    /** Parse reaction JSON into a readable string like "❤️ 3  👍 2" */
    fun formatReactions(reactionsJson: String): String {
        if (reactionsJson.isBlank()) return ""
        return runCatching {
            val obj = JSONObject(reactionsJson)
            obj.keys().asSequence().joinToString("  ") { key ->
                "$key ${obj.getInt(key)}"
            }
        }.getOrDefault("")
    }
}
