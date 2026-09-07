package com.family4.app.ui.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.family4.app.data.db.dao.BoardDao
import com.family4.app.data.db.entity.BoardPostEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class FamilyBoardViewModel @Inject constructor(
    private val boardDao: BoardDao
) : ViewModel() {

    val posts = boardDao.getAllPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Selected post type for composer ──────────────────────────────────────
    private val _selectedPostType = MutableStateFlow("chat")
    val selectedPostType: StateFlow<String> = _selectedPostType

    fun setPostType(type: String) {
        _selectedPostType.value = type
    }

    // ── Add post ─────────────────────────────────────────────────────────────
    fun addPost(
        content: String,
        authorId: String = "self",
        authorName: String = "Me",
        postType: String = _selectedPostType.value
    ) {
        if (content.isBlank()) return
        viewModelScope.launch {
            boardDao.insertPost(
                BoardPostEntity(
                    authorId = authorId,
                    authorName = authorName,
                    content = content.trim(),
                    postType = postType
                )
            )
        }
    }

    // ── Seed welcome post on first launch ─────────────────────────────────────
    fun seedWelcomePostIfEmpty() {
        viewModelScope.launch {
            val count = boardDao.getPostCount()
            if (count == 0) {
                boardDao.insertPost(
                    BoardPostEntity(
                        authorId = "system",
                        authorName = "Family4",
                        content = "👋 Welcome to the Family Board! Share updates, announcements, and moments with everyone. Tap the type buttons below to choose what kind of post to make.",
                        postType = "announcement",
                        pinned = true
                    )
                )
            }
        }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────
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

    // ── Formatting helpers ────────────────────────────────────────────────────

    /** Parse reaction JSON into a list of Pair(emoji, count) sorted by count desc */
    fun parseReactions(reactionsJson: String): List<Pair<String, Int>> {
        if (reactionsJson.isBlank()) return emptyList()
        return runCatching {
            val obj = JSONObject(reactionsJson)
            obj.keys().asSequence()
                .map { key -> key to obj.getInt(key) }
                .sortedByDescending { it.second }
                .toList()
        }.getOrDefault(emptyList())
    }

    /** Format reaction JSON into a readable string like "❤️ 3  👍 2" */
    fun formatReactions(reactionsJson: String): String {
        return parseReactions(reactionsJson).joinToString("  ") { "${it.first} ${it.second}" }
    }

    /** Relative timestamp: "just now", "5 min ago", "Yesterday", "Mar 5" */
    fun formatRelativeTime(createdAt: Long): String {
        val diff = System.currentTimeMillis() - createdAt
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${diff / TimeUnit.MINUTES.toMillis(1)} min ago"
            diff < TimeUnit.HOURS.toMillis(2) -> "1 hour ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${diff / TimeUnit.HOURS.toMillis(1)} hours ago"
            diff < TimeUnit.DAYS.toMillis(2) -> "Yesterday"
            else -> {
                val date = java.util.Date(createdAt)
                java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(date)
            }
        }
    }

    /** Emoji and label for a post type */
    fun postTypeLabel(type: String): Pair<String, String> = when (type) {
        "announcement" -> "📢" to "Announcement"
        "event"        -> "🎉" to "Event"
        "photo"        -> "📸" to "Photo"
        "task"         -> "✅" to "Task"
        else           -> "💬" to "Chat"
    }

    /** Color resource id for post type accent */
    fun postTypeColor(type: String): Int = when (type) {
        "announcement" -> android.graphics.Color.parseColor("#FFFFAA00")  // amber
        "event"        -> android.graphics.Color.parseColor("#FF7B2FFF")  // purple
        "photo"        -> android.graphics.Color.parseColor("#FF00E096")  // green
        "task"         -> android.graphics.Color.parseColor("#FF00D4FF")  // cyan
        else           -> android.graphics.Color.parseColor("#FF3B82D4")  // blue
    }

    /** First 1-2 chars of author name for avatar initials */
    fun initials(name: String): String {
        val parts = name.trim().split(" ")
        return if (parts.size >= 2) {
            "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}".uppercase()
        } else {
            name.take(2).uppercase()
        }
    }

    /** Deterministic color for an author ID (for avatar background) */
    fun avatarColor(authorId: String): Int {
        val colors = listOf(
            "#FF00D4FF", "#FF7B2FFF", "#FFFF3D71", "#FF00E096",
            "#FFFFAA00", "#FF3B82D4", "#FFFF6B6B", "#FF4ECDC4"
        )
        val index = (authorId.hashCode() % colors.size + colors.size) % colors.size
        return android.graphics.Color.parseColor(colors[index])
    }
}
