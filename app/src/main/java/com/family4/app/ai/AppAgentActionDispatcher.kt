package com.family4.app.ai

import android.content.Context
import android.util.Log
import androidx.navigation.NavController
import com.family4.app.R
import com.family4.app.data.db.dao.CalendarDao
import com.family4.app.data.db.dao.NoteDao
import com.family4.app.data.db.dao.TaskDao
import com.family4.app.data.db.entity.CalendarEventEntity
import com.family4.app.data.db.entity.NoteEntity
import com.family4.app.data.db.entity.TaskEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppAgentActionDispatcher — Concrete implementation of AgentActionDispatcher.
 *
 * Receives parsed [FamilyAIAssistant.AgentAction] objects and routes them into
 * real app operations: Room DB writes, navigation, service commands.
 *
 * Injected via Hilt. NavController is set at runtime by MainActivity.
 */
@Singleton
class AppAgentActionDispatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteDao:     NoteDao,
    private val taskDao:     TaskDao,
    private val calendarDao: CalendarDao
) : FamilyAIAssistant.AgentActionDispatcher {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val TAG   = "AgentDispatcher"

    /** Set by MainActivity after NavController is ready */
    var navController: NavController? = null

    override suspend fun dispatch(action: FamilyAIAssistant.AgentAction) {
        Log.d(TAG, "Dispatching action: ${action.type} params=${action.params}")
        when (action.type) {
            "CREATE_NOTE"       -> createNote(action.params)
            "CREATE_TASK"       -> createTask(action.params)
            "CREATE_EVENT"      -> createEvent(action.params)
            "NAVIGATE"          -> navigate(action.params)
            "TRIGGER_SOS"       -> navigate(mapOf("screen" to "sos"))
            "OPEN_CAMERA"       -> navigate(mapOf("screen" to "camera"))
            "SHARE_LOCATION"    -> navigate(mapOf("screen" to "map"))
            "GET_HEALTH_SUMMARY"-> navigate(mapOf("screen" to "health"))
            "GET_WEATHER"       -> navigate(mapOf("screen" to "weather"))
            "GET_FAMILY_STATUS" -> navigate(mapOf("screen" to "home"))
            "SEARCH_NOTES"      -> navigate(mapOf("screen" to "notes"))
            else -> Log.w(TAG, "Unknown action type: ${action.type}")
        }
    }

    // ── Action handlers ───────────────────────────────────────────────────────

    private fun createNote(params: Map<String, String>) {
        scope.launch {
            val title   = params["title"]   ?: "AI Note"
            val content = params["content"] ?: ""
            noteDao.insertNote(
                NoteEntity(
                    title     = title,
                    content   = content,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            Log.d(TAG, "Created note: $title")
        }
    }

    private fun createTask(params: Map<String, String>) {
        scope.launch {
            val title    = params["title"]    ?: "AI Task"
            val priority = when (params["priority"]?.lowercase()) {
                "urgent" -> 3
                "high"   -> 2
                "low"    -> 0
                else     -> 1
            }
            val dueDate = params["due"]?.let { parseDate(it) }
            taskDao.insertTask(
                TaskEntity(
                    title    = title,
                    priority = priority,
                    dueDate  = dueDate,
                    listName = params["list"] ?: "General"
                )
            )
            Log.d(TAG, "Created task: $title priority=$priority")
        }
    }

    private fun createEvent(params: Map<String, String>) {
        scope.launch {
            val title    = params["title"]    ?: "AI Event"
            val dateStr  = params["date"]
            val timeStr  = params["time"]
            val startMs  = parseDateTime(dateStr, timeStr) ?: System.currentTimeMillis()
            calendarDao.insertEvent(
                CalendarEventEntity(
                    title       = title,
                    description = params["description"] ?: "",
                    location    = params["location"]    ?: "",
                    startTime   = startMs,
                    endTime     = startMs + 3_600_000L
                )
            )
            Log.d(TAG, "Created event: $title at $dateStr $timeStr")
        }
    }

    private fun navigate(params: Map<String, String>) {
        val screen = params["screen"]?.lowercase() ?: return
        val destId = when (screen) {
            "home", "dashboard" -> R.id.nav_dashboard
            "notes"             -> R.id.nav_notes
            "chat"              -> R.id.nav_chat_list
            "camera"            -> R.id.nav_camera
            "map"               -> R.id.nav_map
            "walkie"            -> R.id.nav_walkie_talkie
            "sos"               -> R.id.nav_sos
            "health"            -> R.id.nav_health
            "albums"            -> R.id.nav_albums
            "weather"           -> R.id.nav_weather
            "tasks"             -> R.id.nav_tasks
            "settings"          -> R.id.nav_settings
            "files"             -> R.id.nav_files
            "calendar"          -> R.id.nav_calendar
            "ai"                -> R.id.nav_ai_assistant
            else -> { Log.w(TAG, "Unknown screen: $screen"); return }
        }
        // NavController must be posted to main thread
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            navController?.navigate(destId)
        }
    }

    // ── Parsers ───────────────────────────────────────────────────────────────

    private fun parseDate(dateStr: String): Long? = try {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)?.time
    } catch (_: Exception) { null }

    private fun parseDateTime(date: String?, time: String?): Long? {
        if (date == null) return null
        val pattern = if (time != null) "yyyy-MM-dd HH:mm" else "yyyy-MM-dd"
        val input   = if (time != null) "$date $time" else date
        return try {
            SimpleDateFormat(pattern, Locale.US).parse(input)?.time
        } catch (_: Exception) { null }
    }
}
