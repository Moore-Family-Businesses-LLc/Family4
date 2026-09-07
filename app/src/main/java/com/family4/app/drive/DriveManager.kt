package com.family4.app.drive

import android.content.Context
import android.util.Log
import com.family4.app.BuildConfig
import com.family4.app.notifications.NotificationHelper
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DriveManager — handles all Google Drive interactions for Family4.
 *
 * Structure created in Drive:
 *  📁 Family4_AppData/          ← root folder (shared with paulmmoore3416@gmail.com)
 *     📁 Photos/                ← camera photos and album exports
 *     📁 Notes/                 ← note exports (JSON)
 *     📁 Files/                 ← user-uploaded files
 *     📁 Backups/               ← encrypted database backups
 *     📁 Chats/                 ← encrypted chat history exports
 *
 * Every user who signs up gets their own sub-folder under Family4_AppData/{userId}/
 */
@Singleton
class DriveManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var driveService: Drive? = null
    private var family4FolderId: String? = null

    companion object {
        private const val TAG = "DriveManager"
        private const val ROOT_FOLDER_NAME = "Family4_AppData"
        private val REQUIRED_SCOPES = listOf(
            DriveScopes.DRIVE_FILE,     // Access files created by this app
            DriveScopes.DRIVE_APPDATA   // App-private storage
        )
        // Sub-folder names
        const val FOLDER_PHOTOS  = "Photos"
        const val FOLDER_NOTES   = "Notes"
        const val FOLDER_FILES   = "Files"
        const val FOLDER_BACKUPS = "Backups"
        const val FOLDER_CHATS   = "Chats"
    }

    /**
     * Initialise the Drive service with the signed-in Google account.
     * Call this after successful Google Sign-In.
     */
    fun initialize(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, REQUIRED_SCOPES
        ).also { it.selectedAccount = account.account }

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("Family4").build()

        Log.d(TAG, "Drive service initialised for ${account.email}")
    }

    /**
     * Ensures the Family4_AppData root folder exists in the user's Drive.
     * Creates the full folder hierarchy if it doesn't exist.
     * Returns the root folder ID.
     */
    suspend fun ensureFolderStructure(): String? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: run {
                Log.e(TAG, "Drive not initialised"); return@withContext null
            }

            // Find or create root
            val rootId = findOrCreateFolder(service, ROOT_FOLDER_NAME, parent = null)
            family4FolderId = rootId

            // Create sub-folders in parallel
            listOf(FOLDER_PHOTOS, FOLDER_NOTES, FOLDER_FILES, FOLDER_BACKUPS, FOLDER_CHATS)
                .forEach { name -> findOrCreateFolder(service, name, parent = rootId) }

            Log.d(TAG, "Folder structure ready. Root ID: $rootId")
            NotificationHelper.showDriveSyncNotification(
                context, "✅ Family4 Drive folder ready", false
            )
            rootId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create folder structure: ${e.message}")
            NotificationHelper.showDriveSyncNotification(
                context, "❌ Drive sync failed: ${e.message}", true
            )
            null
        }
    }

    /**
     * Uploads a file to the specified sub-folder in Drive.
     *
     * @param localFile  The local java.io.File to upload
     * @param mimeType   e.g. "image/jpeg", "application/json"
     * @param subFolder  One of the FOLDER_* constants above
     * @param driveFileName Optional display name; defaults to local filename
     * @return Google Drive file ID on success, null on failure
     */
    suspend fun uploadFile(
        localFile: java.io.File,
        mimeType: String,
        subFolder: String,
        driveFileName: String = localFile.name
    ): String? = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext null
            val rootId = family4FolderId ?: ensureFolderStructure() ?: return@withContext null

            // Get sub-folder ID
            val subFolderId = findOrCreateFolder(service, subFolder, parent = rootId)

            val fileMetadata = File().apply {
                name = driveFileName
                parents = listOf(subFolderId)
            }
            val mediaContent = FileContent(mimeType, localFile)

            val driveFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id,name,size,webViewLink")
                .execute()

            Log.d(TAG, "Uploaded ${localFile.name} → Drive ID: ${driveFile.id}")
            NotificationHelper.showDriveSyncNotification(
                context, "Uploaded: ${localFile.name}", false
            )
            driveFile.id
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed for ${localFile.name}: ${e.message}")
            NotificationHelper.showDriveSyncNotification(
                context, "Upload failed: ${localFile.name}", true
            )
            null
        }
    }

    /**
     * Lists files in a given sub-folder.
     */
    suspend fun listFiles(subFolder: String): List<DriveFileInfo> = withContext(Dispatchers.IO) {
        try {
            val service = driveService ?: return@withContext emptyList()
            val rootId = family4FolderId ?: ensureFolderStructure() ?: return@withContext emptyList()
            val subFolderId = findOrCreateFolder(service, subFolder, parent = rootId)

            val result = service.files().list()
                .setQ("'$subFolderId' in parents and trashed = false")
                .setFields("files(id,name,size,mimeType,createdTime,webViewLink)")
                .setOrderBy("createdTime desc")
                .execute()

            result.files.map { f ->
                DriveFileInfo(
                    id          = f.id ?: "",
                    name        = f.name ?: "",
                    mimeType    = f.mimeType ?: "",
                    size        = f.getSize() ?: 0L,
                    webViewLink = f.webViewLink ?: "",
                    createdTime = f.createdTime?.value ?: 0L
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "List files failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Downloads a file from Drive to a local cache path.
     */
    suspend fun downloadFile(driveFileId: String, localPath: java.io.File): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val service = driveService ?: return@withContext false
                val outputStream = java.io.FileOutputStream(localPath)
                service.files().get(driveFileId).executeMediaAndDownloadTo(outputStream)
                outputStream.close()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}")
                false
            }
        }

    /**
     * Deletes a file from Drive.
     */
    suspend fun deleteFile(driveFileId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            driveService?.files()?.delete(driveFileId)?.execute()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed: ${e.message}")
            false
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private fun findOrCreateFolder(service: Drive, name: String, parent: String?): String {
        // Search for existing
        val query = buildString {
            append("mimeType = 'application/vnd.google-apps.folder'")
            append(" and name = '$name'")
            append(" and trashed = false")
            if (parent != null) append(" and '$parent' in parents")
        }
        val results = service.files().list().setQ(query).setFields("files(id)").execute()
        if (results.files.isNotEmpty()) {
            return results.files.first().id
        }
        // Create it
        val folderMetadata = File().apply {
            this.name = name
            mimeType = "application/vnd.google-apps.folder"
            if (parent != null) parents = listOf(parent)
        }
        val created = service.files().create(folderMetadata).setFields("id").execute()
        Log.d(TAG, "Created folder '$name' with ID: ${created.id}")
        return created.id
    }

    // ── Data Classes ──────────────────────────────────────────────────────────
    data class DriveFileInfo(
        val id: String,
        val name: String,
        val mimeType: String,
        val size: Long,
        val webViewLink: String,
        val createdTime: Long
    )
}
