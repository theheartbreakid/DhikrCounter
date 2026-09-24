package com.Crescent.DhikrCounter.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.Crescent.DhikrCounter.core.update.model.UpdateAsset
import com.Crescent.DhikrCounter.core.update.model.UpdateError
import com.Crescent.DhikrCounter.core.update.model.UpdateInfo
import com.Crescent.DhikrCounter.core.update.model.UpdateState
import com.Crescent.DhikrCounter.utils.SettingsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL


class UpdateManager(
    private val context: Context,
    private val settingsManager: SettingsManager
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var downloadJob: Job? = null

    data class Version(
        val major: Int,
        val minor: Int,
        val patch: Int
    )

    companion object {
        private const val TAG = "DhikrUpdate"
        const val REPO_OWNER = "theheartbreakid"
        const val REPO_NAME = "DhikrCounter"
        const val API_LATEST_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
        const val RELEASES_PAGE_URL = "https://github.com/$REPO_OWNER/$REPO_NAME/releases/latest"

        private const val CONNECT_TIMEOUT_MS = 15000
        private const val READ_TIMEOUT_MS = 20000

        /**
         * Parses a version string like "1.0.0", "v1.0.0", "Version 1.0.0", "Release 1.0.0"
         * into a numeric Version(major, minor, patch).
         * Returns null if no valid X.Y.Z version can be extracted.
         */
        fun parseReleaseVersion(raw: String): Version? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null

            val match = Regex("""(?:v|version|release|build)?\s*(\d+)\.(\d+)\.(\d+)""", RegexOption.IGNORE_CASE)
                .find(trimmed) ?: return null

            val major = match.groupValues[1].toIntOrNull() ?: return null
            val minor = match.groupValues[2].toIntOrNull() ?: return null
            val patch = match.groupValues[3].toIntOrNull() ?: return null

            return Version(major, minor, patch)
        }

        /**
         * Compares current and remote versions.
         * Returns:
         *   > 0 if remote > current (update available)
         *  == 0 if remote == current (up to date)
         *   < 0 if remote < current (installed is newer)
         */
        fun compareVersions(current: Version, remote: Version): Int {
            if (remote.major != current.major) {
                return remote.major.compareTo(current.major)
            }
            if (remote.minor != current.minor) {
                return remote.minor.compareTo(current.minor)
            }
            return remote.patch.compareTo(current.patch)
        }
    }

    init {
        checkAndCleanupAfterAppStartup()
    }

    /**
     * Clean up leftover downloaded update APKs if installed version >= pending version.
     */
    fun checkAndCleanupAfterAppStartup() {
        val currentVersionStr = getCurrentVersionName()
        val pendingVersionStr = settingsManager.pendingUpdateVersionName
        val downloadedPath = settingsManager.downloadedApkPath

        val currentVer = parseReleaseVersion(currentVersionStr)
        val pendingVer = parseReleaseVersion(pendingVersionStr)

        val shouldClean = when {
            pendingVersionStr.isEmpty() -> false
            currentVer != null && pendingVer != null -> compareVersions(currentVer, pendingVer) <= 0
            currentVersionStr == pendingVersionStr -> true
            else -> false
        }

        if (shouldClean) {
            if (downloadedPath.isNotEmpty()) {
                val file = File(downloadedPath)
                if (file.exists()) {
                    file.delete()
                }
            }
            settingsManager.clearPendingUpdate()
        }
    }

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    /**
     * Check GitHub Releases for updates using semantic versioning.
     */
    suspend fun checkForUpdates(isManual: Boolean = false): UpdateState = withContext(Dispatchers.IO) {
        if (_updateState.value is UpdateState.Checking) {
            Log.d(TAG, "Update check already in progress. Ignoring duplicate request.")
            return@withContext _updateState.value
        }

        _updateState.value = UpdateState.Checking
        Log.i(TAG, "Update check started. Request URL: $API_LATEST_URL")

        try {
            val currentVersionStr = getCurrentVersionName()
            val currentVersion = parseReleaseVersion(currentVersionStr)

            // 1. Fetch latest release from GitHub API
            val releaseObj = fetchLatestRelease()

            val releaseId = releaseObj.optLong("id", 0L)
            val tagName = releaseObj.optString("tag_name", "")
            val releaseName = releaseObj.optString("name", tagName)
            val releaseBody = releaseObj.optString("body", "")
            val publishedAt = releaseObj.optString("published_at", "")
            val htmlUrl = releaseObj.optString("html_url", RELEASES_PAGE_URL)

            Log.i(TAG, "Latest release fetched: Tag: $tagName, Name: $releaseName")

            // 2. Extract MAJOR.MINOR.PATCH from release tag_name or name
            val remoteVersion = parseReleaseVersion(tagName)
                ?: parseReleaseVersion(releaseName)

            if (remoteVersion == null) {
                val error = UpdateError.ReleaseVersionError()
                val state = UpdateState.CheckError(error)
                _updateState.value = state
                Log.w(TAG, "Unable to extract semantic version from release tag '$tagName' or name '$releaseName'")
                return@withContext state
            }

            val remoteVersionStr = "${remoteVersion.major}.${remoteVersion.minor}.${remoteVersion.patch}"
            Log.i(TAG, "Parsed versions - Current: '$currentVersionStr' ($currentVersion), Remote: '$remoteVersionStr' ($remoteVersion)")

            // 3. Find APK asset in release
            val assetsArray = releaseObj.optJSONArray("assets") ?: JSONArray()
            val assetsList = mutableListOf<UpdateAsset>()
            for (i in 0 until assetsArray.length()) {
                val a = assetsArray.getJSONObject(i)
                val aName = a.optString("name", "")
                val aSize = a.optLong("size", 0L)
                val aUrl = a.optString("browser_download_url", "")
                val isApk = aName.endsWith(".apk", ignoreCase = true)
                assetsList.add(UpdateAsset(aName, aSize, aUrl, isApk))
            }

            val apkAsset = selectBestApkAsset(assetsList)
            if (apkAsset == null) {
                val error = UpdateError.ApkNotFound(tagName)
                val state = UpdateState.CheckError(error)
                _updateState.value = state
                Log.w(TAG, "No APK asset found in release $tagName")
                return@withContext state
            }

            // 4. Compare current vs remote version
            val isUpdateAvailable = if (currentVersion != null) {
                compareVersions(currentVersion, remoteVersion) > 0
            } else {
                // Fallback string inequality if currentVersion couldn't be parsed as semver
                currentVersionStr != remoteVersionStr
            }

            // Update timestamp on successful check
            settingsManager.lastUpdateCheckTimestamp = System.currentTimeMillis()

            if (isUpdateAvailable) {
                val updateInfo = UpdateInfo(
                    releaseId = releaseId,
                    tagName = tagName,
                    releaseName = releaseName,
                    releaseNotes = releaseBody,
                    publishedAt = publishedAt,
                    releaseUrl = htmlUrl,
                    apkAsset = apkAsset,
                    remoteVersionName = remoteVersionStr
                )
                val state = UpdateState.UpdateAvailable(updateInfo)
                _updateState.value = state
                return@withContext state
            } else {
                val state = UpdateState.UpToDate(currentVersionStr)
                _updateState.value = state
                return@withContext state
            }
        } catch (e: UpdateException) {
            val state = UpdateState.CheckError(e.error)
            _updateState.value = state
            Log.e(TAG, "Update check failed: ${e.error.message}")
            return@withContext state
        } catch (e: Exception) {
            val state = UpdateState.CheckError(UpdateError.NetworkError())
            _updateState.value = state
            Log.e(TAG, "Update check failed with exception: ${e.message}", e)
            return@withContext state
        }
    }

    private class UpdateException(val error: UpdateError) : Exception(error.message)

    /**
     * Resolves the latest release from GitHub API.
     */
    private fun fetchLatestRelease(): JSONObject {
        val (responseBody, code) = executeHttpRequest(API_LATEST_URL)

        if (code !in 200..299) {
            throw UpdateException(UpdateError.NetworkError())
        }

        return try {
            JSONObject(responseBody)
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error for /releases/latest: ${e.message}")
            throw UpdateException(UpdateError.ReleaseVersionError())
        }
    }

    private data class HttpResponse(val body: String, val code: Int)

    private fun executeHttpRequest(urlString: String): HttpResponse {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                val safeVer = getCurrentVersionName().filter { it.isLetterOrDigit() || it == '.' || it == '-' }
                setRequestProperty("User-Agent", "DhikrCounter-App/$safeVer")
            }

            val responseCode = connection.responseCode
            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }

            return HttpResponse(responseBody, responseCode)
        } catch (e: Throwable) {
            throw UpdateException(UpdateError.NetworkError())
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Selects the best production APK asset.
     */
    fun selectBestApkAsset(assets: List<UpdateAsset>): UpdateAsset? {
        val apkAssets = assets.filter { it.isApk }
        if (apkAssets.isEmpty()) return null

        val productionApks = apkAssets.filter {
            !it.name.contains("debug", ignoreCase = true) &&
            !it.name.contains("test", ignoreCase = true)
        }

        val candidates = if (productionApks.isNotEmpty()) productionApks else apkAssets

        return candidates.firstOrNull { it.name.contains("release", ignoreCase = true) }
            ?: candidates.firstOrNull { it.name.startsWith("DhikrCounter", ignoreCase = true) }
            ?: candidates.firstOrNull()
    }

    /**
     * Downloads the APK file to a dedicated update directory inside filesDir.
     */
    fun startDownload(updateInfo: UpdateInfo, scope: CoroutineScope) {
        downloadJob?.cancel()
        downloadJob = scope.launch(Dispatchers.IO) {
            performDownload(updateInfo)
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _updateState.value = UpdateState.Idle
    }

    private suspend fun performDownload(updateInfo: UpdateInfo) = withContext(Dispatchers.IO) {
        val updateDir = File(context.filesDir, "updates").apply { mkdirs() }

        updateDir.listFiles()?.forEach { file ->
            if (file.name != updateInfo.apkAsset.name) {
                file.delete()
            }
        }

        val targetFile = File(updateDir, updateInfo.apkAsset.name)
        val tempFile = File(updateDir, "${updateInfo.apkAsset.name}.download")

        if (targetFile.exists() && validateDownloadedApk(targetFile) == null) {
            _updateState.value = UpdateState.DownloadComplete(updateInfo, targetFile.absolutePath)
            return@withContext
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(updateInfo.apkAsset.downloadUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                val safeVer = getCurrentVersionName().filter { it.isLetterOrDigit() || it == '.' || it == '-' }
                setRequestProperty("User-Agent", "DhikrCounter-App/$safeVer")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("HTTP error $responseCode while downloading update.")
            }

            val totalBytes = if (connection.contentLengthLong > 0) connection.contentLengthLong else updateInfo.apkAsset.size
            var downloadedBytes = 0L
            var lastProgressTime = System.currentTimeMillis()
            var bytesSinceLastTime = 0L
            var speedBytesPerSec = 0L

            tempFile.outputStream().use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(16 * 1024)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        ensureActive()
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        bytesSinceLastTime += read

                        val now = System.currentTimeMillis()
                        val diffTime = now - lastProgressTime
                        if (diffTime >= 500) {
                            speedBytesPerSec = (bytesSinceLastTime * 1000) / diffTime
                            lastProgressTime = now
                            bytesSinceLastTime = 0L

                            val percent = if (totalBytes > 0) {
                                ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }

                            _updateState.value = UpdateState.Downloading(
                                info = updateInfo,
                                progressPercent = percent,
                                bytesDownloaded = downloadedBytes,
                                totalBytes = totalBytes,
                                speedBytesPerSec = speedBytesPerSec
                            )
                        }
                    }
                    output.flush()
                }
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                throw IOException("Failed to finalize downloaded APK.")
            }

            val validationError = validateDownloadedApk(targetFile)
            if (validationError != null) {
                targetFile.delete()
                _updateState.value = UpdateState.ValidationError(validationError)
                return@withContext
            }

            settingsManager.pendingUpdateVersionName = updateInfo.remoteVersionName
            settingsManager.downloadedApkPath = targetFile.absolutePath

            _updateState.value = UpdateState.DownloadComplete(updateInfo, targetFile.absolutePath)

        } catch (e: CancellationException) {
            tempFile.delete()
            _updateState.value = UpdateState.Idle
        } catch (e: Exception) {
            tempFile.delete()
            val msg = e.localizedMessage ?: "Download failed."
            _updateState.value = UpdateState.DownloadError(msg, canRetry = true)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Minimum necessary APK validation before prompting installation:
     * - File exists and is readable
     * - Package structure is valid and matches this app's package name
     */
    fun validateDownloadedApk(apkFile: File): String? {
        if (!apkFile.exists() || !apkFile.canRead() || apkFile.length() == 0L) {
            return "Downloaded file is missing, empty, or unreadable."
        }

        val pm = context.packageManager
        val archiveInfo: PackageInfo? = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)

        if (archiveInfo == null) {
            return "Corrupted APK: package information could not be parsed."
        }

        val apkPackageName = archiveInfo.packageName
        if (apkPackageName != context.packageName) {
            return "Package mismatch: APK is for '$apkPackageName', expected '${context.packageName}'."
        }

        return null
    }

    fun getDownloadedApkPathIfValid(info: UpdateInfo): String? {
        val savedPath = settingsManager.downloadedApkPath
        if (savedPath.isNotEmpty()) {
            val file = File(savedPath)
            if (file.exists() && validateDownloadedApk(file) == null) {
                return savedPath
            }
        }
        return null
    }

    fun setWaitingForPermission(info: UpdateInfo, apkPath: String) {
        _updateState.value = UpdateState.WaitingForInstallPermission(info, apkPath)
    }

    /**
     * Checks if the app has permission to install unknown apps (Android 8.0+).
     */
    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Creates intent to take the user to the app-specific Manage Unknown Apps screen.
     */
    fun getManageUnknownAppSourcesIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }

    /**
     * Launches the system package installer for the downloaded APK using a secure content:// URI.
     */
    fun launchInstaller(apkPath: String) {
        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            _updateState.value = UpdateState.ValidationError("APK file does not exist at $apkPath")
            return
        }

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            _updateState.value = UpdateState.Installing(
                UpdateInfo(
                    releaseId = 0L,
                    tagName = "",
                    releaseName = "",
                    releaseNotes = "",
                    publishedAt = "",
                    releaseUrl = "",
                    apkAsset = UpdateAsset(apkFile.name, apkFile.length(), "", true),
                    remoteVersionName = ""
                ),
                apkPath
            )

            context.startActivity(installIntent)
        } catch (e: Exception) {
            _updateState.value = UpdateState.ValidationError("Failed to launch package installer: ${e.message}")
        }
    }
}

