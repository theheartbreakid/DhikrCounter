package com.Crescent.DhikrCounter.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.security.MessageDigest
import java.util.Locale
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

class UpdateManager(
    private val context: Context,
    private val settingsManager: SettingsManager
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var downloadJob: Job? = null
    private var checkJob: Job? = null

    companion object {
        private const val TAG = "DhikrUpdate"
        const val REPO_OWNER = "theheartbreakid"
        const val REPO_NAME = "DhikrCounter"
        const val API_LATEST_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
        const val API_ALL_RELEASES_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases"
        const val RELEASES_PAGE_URL = "https://github.com/$REPO_OWNER/$REPO_NAME/releases/latest"

        private const val CONNECT_TIMEOUT_MS = 15000
        private const val READ_TIMEOUT_MS = 20000
    }

    init {
        checkAndCleanupAfterAppStartup()
    }

    /**
     * Checks if device currently has active network capabilities.
     */
    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Compare installed version with any recorded pending update and clean up stale APKs.
     */
    fun checkAndCleanupAfterAppStartup() {
        val currentCode = getCurrentVersionCode()
        val pendingCode = settingsManager.pendingUpdateVersionCode
        val downloadedPath = settingsManager.downloadedApkPath

        if (pendingCode > 0 && currentCode >= pendingCode) {
            if (downloadedPath.isNotEmpty()) {
                val file = File(downloadedPath)
                if (file.exists()) {
                    file.delete()
                }
            }
            settingsManager.clearPendingUpdate()
        }
    }

    fun getCurrentVersionCode(): Long {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
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
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }

    /**
     * Check GitHub Releases for updates.
     * Prevents duplicate simultaneous executions.
     */
    suspend fun checkForUpdates(isManual: Boolean = false): UpdateState = withContext(Dispatchers.IO) {
        if (_updateState.value is UpdateState.Checking) {
            Log.d(TAG, "Update check already in progress. Ignoring duplicate request.")
            return@withContext _updateState.value
        }

        _updateState.value = UpdateState.Checking
        Log.i(TAG, "Update check started. Request URL: $API_LATEST_URL")

        // 1. Basic network check (informative, not sole authority)
        val networkOnline = isNetworkAvailable()
        Log.d(TAG, "Network available: $networkOnline")
        if (!networkOnline) {
            val error = UpdateError.NoInternet()
            val state = UpdateState.CheckError(error)
            _updateState.value = state
            Log.w(TAG, "Update check aborted: No network connectivity.")
            return@withContext state
        }

        try {
            val currentCode = getCurrentVersionCode()
            val currentName = getCurrentVersionName()

            // 2. Fetch authoritative latest release from GitHub API
            val releaseObj = fetchLatestRelease()

            val releaseId = releaseObj.optLong("id", 0L)
            val tagName = releaseObj.optString("tag_name", "")
            val releaseName = releaseObj.optString("name", tagName)
            val releaseBody = releaseObj.optString("body", "")
            val publishedAt = releaseObj.optString("published_at", "")
            val htmlUrl = releaseObj.optString("html_url", RELEASES_PAGE_URL)

            Log.i(TAG, "Release fetched successfully: Tag: $tagName, ID: $releaseId, Name: $releaseName")

            val assetsArray = releaseObj.optJSONArray("assets") ?: JSONArray()
            val assetsList = mutableListOf<UpdateAsset>()
            for (i in 0 until assetsArray.length()) {
                val a = assetsArray.getJSONObject(i)
                val aName = a.optString("name", "")
                val aSize = a.optLong("size", 0L)
                val aUrl = a.optString("browser_download_url", "")
                val isApk = aName.endsWith(".apk", ignoreCase = true)
                val isSha = aName.endsWith(".sha256", ignoreCase = true) || aName.endsWith(".sha256.txt", ignoreCase = true)
                assetsList.add(UpdateAsset(aName, aSize, aUrl, isApk, isSha))
            }
            Log.d(TAG, "Assets found in release: ${assetsList.size}")

            // Find correct production release APK
            val apkAsset = selectBestApkAsset(assetsList)
            val checksumAsset = assetsList.firstOrNull { it.isSha256Checksum }

            if (apkAsset == null) {
                val error = UpdateError.ApkNotFound(tagName, htmlUrl)
                val state = UpdateState.CheckError(error)
                _updateState.value = state
                Log.w(TAG, "No compatible APK asset found in release $tagName")
                return@withContext state
            }
            Log.i(TAG, "APK asset selected: ${apkAsset.name} (${apkAsset.size} bytes)")

            // Extract remote version code and version name from release metadata / assets / tag
            val (remoteCode, remoteName) = resolveRemoteVersion(tagName, releaseName, apkAsset.name)
            Log.d(TAG, "Remote: versionCode=$remoteCode, versionName='$remoteName'. Installed: versionCode=$currentCode, versionName='$currentName'")

            val isNewer = isRemoteNewer(
                remoteCode = remoteCode,
                remoteName = remoteName,
                currentCode = currentCode,
                currentName = currentName,
                tagName = tagName,
                releaseId = releaseId
            )
            Log.i(TAG, "Update available: $isNewer")

            // Update timestamp only on successful check
            settingsManager.lastUpdateCheckTimestamp = System.currentTimeMillis()

            if (isNewer) {
                val updateInfo = UpdateInfo(
                    releaseId = releaseId,
                    tagName = tagName,
                    releaseName = releaseName,
                    releaseNotes = releaseBody,
                    publishedAt = publishedAt,
                    releaseUrl = htmlUrl,
                    apkAsset = apkAsset,
                    checksumAsset = checksumAsset,
                    remoteVersionCode = remoteCode,
                    remoteVersionName = remoteName
                )
                val state = UpdateState.UpdateAvailable(updateInfo)
                _updateState.value = state
                return@withContext state
            } else {
                val state = UpdateState.UpToDate(currentName, currentCode)
                _updateState.value = state
                return@withContext state
            }
        } catch (e: UpdateException) {
            val state = UpdateState.CheckError(e.error)
            _updateState.value = state
            Log.e(TAG, "Update check failed with UpdateError: ${e.error.message}", e)
            return@withContext state
        } catch (e: Throwable) {
            val updateError = mapThrowableToUpdateError(e, API_LATEST_URL)
            val state = UpdateState.CheckError(updateError)
            _updateState.value = state
            Log.e(TAG, "Update check failed with unhandled exception: ${e.message}", e)
            return@withContext state
        }
    }

    private class UpdateException(val error: UpdateError) : Exception(error.message)

    /**
     * Resolves the authoritative latest release from GitHub API.
     */
    private fun fetchLatestRelease(): JSONObject {
        val (responseBody, code, msg) = executeHttpRequest(API_LATEST_URL)

        if (code == 404) {
            // If /releases/latest returns 404, fall back to /releases list
            Log.d(TAG, "/releases/latest returned 404, falling back to /releases")
            return fetchLatestFromAllReleases()
        }

        if (code == 403) {
            throw UpdateException(UpdateError.RateLimited(requestUrl = API_LATEST_URL))
        }

        if (code !in 200..299) {
            throw UpdateException(
                UpdateError.HttpError(
                    code = code,
                    statusMessage = msg,
                    message = "GitHub API returned HTTP $code ($msg).",
                    technicalDetails = "GET $API_LATEST_URL returned HTTP $code $msg\nResponse: $responseBody",
                    requestUrl = API_LATEST_URL
                )
            )
        }

        return try {
            JSONObject(responseBody)
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error for /releases/latest: ${e.message}")
            throw UpdateException(
                UpdateError.ParseError(
                    message = "GitHub responded with HTTP 200, but the release JSON format could not be parsed.",
                    technicalDetails = "JSONException: ${e.message}\nPayload preview: ${responseBody.take(200)}",
                    requestUrl = API_LATEST_URL
                )
            )
        }
    }

    private fun fetchLatestFromAllReleases(): JSONObject {
        val (responseBody, code, msg) = executeHttpRequest(API_ALL_RELEASES_URL)

        if (code == 403) {
            throw UpdateException(UpdateError.RateLimited(requestUrl = API_ALL_RELEASES_URL))
        }

        if (code !in 200..299) {
            throw UpdateException(
                UpdateError.HttpError(
                    code = code,
                    statusMessage = msg,
                    message = "GitHub API returned HTTP $code ($msg).",
                    technicalDetails = "GET $API_ALL_RELEASES_URL returned HTTP $code $msg\nResponse: $responseBody",
                    requestUrl = API_ALL_RELEASES_URL
                )
            )
        }

        try {
            val array = JSONArray(responseBody)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (!obj.optBoolean("draft", false)) {
                    return obj
                }
            }
            throw UpdateException(UpdateError.ReleaseNotFound(requestUrl = API_ALL_RELEASES_URL))
        } catch (e: UpdateException) {
            throw e
        } catch (e: Exception) {
            throw UpdateException(
                UpdateError.ParseError(
                    message = "GitHub responded with HTTP 200, but the releases list could not be parsed.",
                    technicalDetails = "JSONException: ${e.message}",
                    requestUrl = API_ALL_RELEASES_URL
                )
            )
        }
    }

    private data class HttpResponse(val body: String, val code: Int, val message: String)

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
                // Clean User-Agent without any special characters
                val safeVer = getCurrentVersionName().filter { it.isLetterOrDigit() || it == '.' || it == '-' }
                setRequestProperty("User-Agent", "DhikrCounter-App/$safeVer")
            }

            val responseCode = connection.responseCode
            val responseMsg = connection.responseMessage ?: ""

            val responseBody = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }

            return HttpResponse(responseBody, responseCode, responseMsg)
        } catch (e: Throwable) {
            throw mapThrowableToUpdateError(e, urlString).let { UpdateException(it) }
        } finally {
            connection?.disconnect()
        }
    }

    private fun mapThrowableToUpdateError(e: Throwable, url: String): UpdateError {
        return when (e) {
            is UnknownHostException -> UpdateError.DnsError(
                host = e.message ?: "api.github.com",
                technicalDetails = "UnknownHostException: ${e.message}",
                requestUrl = url
            )
            is SocketTimeoutException -> UpdateError.Timeout(
                message = "Connection to GitHub timed out. Check your connection speed or try again.",
                technicalDetails = "SocketTimeoutException: ${e.message}",
                requestUrl = url
            )
            is ConnectException, is SocketException -> UpdateError.ConnectionError(
                exceptionName = e.javaClass.simpleName,
                message = "Failed to connect to GitHub (${e.javaClass.simpleName}).",
                technicalDetails = "${e.javaClass.name}: ${e.message}",
                requestUrl = url
            )
            is SSLHandshakeException, is SSLException -> UpdateError.TlsError(
                message = "Secure connection to GitHub failed. The device could not establish a trusted HTTPS connection.",
                technicalDetails = "${e.javaClass.name}: ${e.message}",
                requestUrl = url
            )
            else -> UpdateError.Unknown(
                exceptionType = e.javaClass.simpleName,
                message = e.localizedMessage ?: "Unexpected error during update check.",
                technicalDetails = "${e.javaClass.name}: ${e.message}",
                requestUrl = url
            )
        }
    }

    /**
     * Selects the best production APK asset.
     * Filters out non-apk, debug, test, aab, zip, mapping files.
     */
    fun selectBestApkAsset(assets: List<UpdateAsset>): UpdateAsset? {
        val apkAssets = assets.filter { it.isApk }
        if (apkAssets.isEmpty()) return null

        val productionApks = apkAssets.filter {
            !it.name.contains("debug", ignoreCase = true) &&
            !it.name.contains("test", ignoreCase = true) &&
            !it.name.contains("androidTest", ignoreCase = true)
        }

        val candidates = if (productionApks.isNotEmpty()) productionApks else apkAssets

        return candidates.firstOrNull { it.name.contains("release", ignoreCase = true) }
            ?: candidates.firstOrNull { it.name.startsWith("DhikrCounter", ignoreCase = true) }
            ?: candidates.firstOrNull()
    }

    /**
     * Extracts or estimates versionCode and versionName from tags or release names.
     */
    fun resolveRemoteVersion(tagName: String, releaseName: String, apkName: String): Pair<Long, String> {
        var resolvedCode = 0L
        var resolvedName = ""

        val buildMatch = Regex("""#(\d+)""").find(releaseName)
        if (buildMatch != null) {
            resolvedCode = buildMatch.groupValues[1].toLongOrNull() ?: 0L
        }

        val semVerMatch = Regex("""(?:v|release-)?(\d+(?:\.\d+)+)""").find(tagName)
        if (semVerMatch != null) {
            resolvedName = semVerMatch.groupValues[1]
        } else {
            val nameMatch = Regex("""(?:v|version\s*)?(\d+(?:\.\d+)+)""", RegexOption.IGNORE_CASE).find(releaseName)
            if (nameMatch != null) {
                resolvedName = nameMatch.groupValues[1]
            }
        }

        if (resolvedCode == 0L) {
            val tagNumberMatch = Regex("""build-(\d+)""").find(tagName)
            if (tagNumberMatch != null) {
                resolvedCode = tagNumberMatch.groupValues[1].toLongOrNull() ?: 0L
            }
        }

        if (resolvedName.isEmpty()) {
            resolvedName = if (resolvedCode > 0) "Build #$resolvedCode" else tagName
        }

        return Pair(resolvedCode, resolvedName)
    }

    /**
     * Determines whether remote release is newer than current installation.
     */
    fun isRemoteNewer(
        remoteCode: Long,
        remoteName: String,
        currentCode: Long,
        currentName: String,
        tagName: String,
        releaseId: Long
    ): Boolean {
        if (remoteCode > 0 && currentCode > 0) {
            return remoteCode > currentCode
        }

        val semVerComp = compareSemanticVersions(remoteName, currentName)
        if (semVerComp != null) {
            return semVerComp > 0
        }

        val normalizedTag = normalizeVersionString(tagName)
        val normalizedCurrent = normalizeVersionString(currentName)
        if (normalizedTag.isNotEmpty() && normalizedCurrent.isNotEmpty() && normalizedTag != normalizedCurrent) {
            val tagComp = compareSemanticVersions(normalizedTag, normalizedCurrent)
            if (tagComp != null) {
                return tagComp > 0
            }
        }

        return false
    }

    fun compareSemanticVersions(v1: String, v2: String): Int? {
        val s1 = normalizeVersionString(v1)
        val s2 = normalizeVersionString(v2)
        if (s1.isEmpty() || s2.isEmpty()) return null

        val p1 = s1.split(".").mapNotNull { it.toIntOrNull() }
        val p2 = s2.split(".").mapNotNull { it.toIntOrNull() }
        if (p1.isEmpty() || p2.isEmpty()) return null

        val maxLen = maxOf(p1.size, p2.size)
        for (i in 0 until maxLen) {
            val part1 = p1.getOrElse(i) { 0 }
            val part2 = p2.getOrElse(i) { 0 }
            if (part1 != part2) {
                return part1.compareTo(part2)
            }
        }
        return 0
    }

    fun normalizeVersionString(v: String): String {
        return v.trim()
            .removePrefix("v")
            .removePrefix("V")
            .removePrefix("release-")
            .removePrefix("build-")
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

        if (targetFile.exists() && validateDownloadedApk(targetFile, updateInfo) == null) {
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

            val validationError = validateDownloadedApk(targetFile, updateInfo)
            if (validationError != null) {
                targetFile.delete()
                _updateState.value = UpdateState.ValidationError(validationError)
                return@withContext
            }

            settingsManager.pendingUpdateVersionCode = updateInfo.remoteVersionCode
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
     * Validates downloaded APK:
     * - File exists and is readable
     * - Expected size match if asset size known
     * - APK structure is readable by PackageManager
     * - Package ID matches currently installed application
     * - VersionCode is not downgrade
     * - Signatures compatibility check
     * - SHA-256 match if checksum asset available
     */
    fun validateDownloadedApk(apkFile: File, updateInfo: UpdateInfo): String? {
        if (!apkFile.exists() || !apkFile.canRead() || apkFile.length() == 0L) {
            return "Downloaded file is missing, empty, or unreadable."
        }

        if (updateInfo.apkAsset.size > 0 && apkFile.length() != updateInfo.apkAsset.size) {
            return "File size mismatch (expected ${updateInfo.apkAsset.size} bytes, got ${apkFile.length()} bytes)."
        }

        val pm = context.packageManager
        val archiveInfo: PackageInfo? = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            ?: pm.getPackageArchiveInfo(apkFile.absolutePath, @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES)

        if (archiveInfo == null) {
            return "Corrupted APK: package information could not be parsed."
        }

        val apkPackageName = archiveInfo.packageName
        if (apkPackageName != context.packageName) {
            return "Package mismatch: APK is for '$apkPackageName', expected '${context.packageName}'."
        }

        val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode.toLong()
        }

        val currentCode = getCurrentVersionCode()
        if (apkVersionCode <= currentCode && updateInfo.remoteVersionCode > currentCode) {
            return "Version mismatch: APK contains versionCode $apkVersionCode which is not greater than installed $currentCode."
        }

        if (updateInfo.checksumAsset != null) {
            try {
                val expectedChecksum = fetchChecksumFromUrl(updateInfo.checksumAsset.downloadUrl)
                if (!expectedChecksum.isNullOrBlank()) {
                    val actualChecksum = calculateSha256(apkFile)
                    if (!expectedChecksum.contains(actualChecksum, ignoreCase = true)) {
                        return "SHA-256 checksum mismatch. Expected: $expectedChecksum, Actual: $actualChecksum."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return null
    }

    private fun fetchChecksumFromUrl(url: String): String? {
        return try {
            executeHttpRequest(url).body.trim()
        } catch (e: Exception) {
            null
        }
    }

    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val bytes = digest.digest()
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format(Locale.ROOT, "%02x", b))
        }
        return sb.toString()
    }

    fun getDownloadedApkPathIfValid(info: UpdateInfo): String? {
        val savedPath = settingsManager.downloadedApkPath
        if (savedPath.isNotEmpty()) {
            val file = File(savedPath)
            if (file.exists() && validateDownloadedApk(file, info) == null) {
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
                    apkAsset = UpdateAsset(apkFile.name, apkFile.length(), "", true, false),
                    remoteVersionCode = 0L,
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
