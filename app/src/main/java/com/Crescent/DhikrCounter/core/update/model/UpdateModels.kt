package com.Crescent.DhikrCounter.core.update.model

/**
 * Diagnostic error representation for in-app update checks.
 * Distinguishes connectivity, DNS, timeouts, TLS, HTTP errors, JSON parsing, and release/asset detection issues.
 */
sealed class UpdateError(
    open val message: String,
    open val technicalDetails: String? = null,
    open val httpStatus: Int? = null,
    open val requestUrl: String? = null,
    open val canRetry: Boolean = true
) {
    data class NoInternet(
        override val message: String = "No internet connection detected.",
        override val technicalDetails: String? = "Android network capability reports offline."
    ) : UpdateError(message, technicalDetails, canRetry = true)

    data class DnsError(
        val host: String,
        override val message: String = "Unable to reach GitHub. The domain $host could not be resolved.",
        override val technicalDetails: String? = "UnknownHostException: $host",
        override val requestUrl: String? = null
    ) : UpdateError(message, technicalDetails, requestUrl = requestUrl, canRetry = true)

    data class Timeout(
        override val message: String = "Connection to GitHub timed out.",
        override val technicalDetails: String? = "SocketTimeoutException: Connection or read timeout expired.",
        override val requestUrl: String? = null
    ) : UpdateError(message, technicalDetails, requestUrl = requestUrl, canRetry = true)

    data class ConnectionError(
        val exceptionName: String,
        override val message: String = "Failed to establish a network connection to GitHub.",
        override val technicalDetails: String? = null,
        override val requestUrl: String? = null
    ) : UpdateError(message, technicalDetails, requestUrl = requestUrl, canRetry = true)

    data class TlsError(
        override val message: String = "Secure connection to GitHub failed. The device could not establish a trusted HTTPS connection.",
        override val technicalDetails: String? = "SSLException / SSLHandshakeException",
        override val requestUrl: String? = null
    ) : UpdateError(message, technicalDetails, requestUrl = requestUrl, canRetry = true)

    data class RateLimited(
        val resetTimeSeconds: Long? = null,
        override val message: String = "GitHub API rate limit reached. Please try again later.",
        override val technicalDetails: String? = "HTTP 403 (Rate limited)",
        override val httpStatus: Int = 403,
        override val requestUrl: String? = null
    ) : UpdateError(message, technicalDetails, httpStatus = httpStatus, requestUrl = requestUrl, canRetry = true)

    data class HttpError(
        val code: Int,
        val statusMessage: String?,
        override val message: String,
        override val technicalDetails: String? = "HTTP $code $statusMessage",
        override val requestUrl: String? = null
    ) : UpdateError(message, technicalDetails, httpStatus = code, requestUrl = requestUrl, canRetry = true)

    data class ReleaseNotFound(
        override val message: String = "GitHub was reached successfully, but no release was found for repository theheartbreakid/DhikrCounter.",
        override val technicalDetails: String? = "HTTP 404 or empty release list",
        override val httpStatus: Int? = 404,
        override val requestUrl: String? = null
    ) : UpdateError(message, technicalDetails, httpStatus = httpStatus, requestUrl = requestUrl, canRetry = true)

    data class ParseError(
        override val message: String = "GitHub responded successfully, but the update information could not be read.",
        override val technicalDetails: String? = null,
        override val requestUrl: String? = null
    ) : UpdateError(message, technicalDetails, httpStatus = 200, requestUrl = requestUrl, canRetry = true)

    data class ApkNotFound(
        val tagName: String,
        val releaseUrl: String,
        override val message: String = "GitHub was reached successfully (release $tagName), but no compatible APK asset was found.",
        override val technicalDetails: String? = "Release $tagName contains no production APK assets.",
        override val requestUrl: String? = releaseUrl
    ) : UpdateError(message, technicalDetails, requestUrl = requestUrl, canRetry = true)

    data class Unknown(
        val exceptionType: String,
        override val message: String,
        override val technicalDetails: String? = null,
        override val requestUrl: String? = null
    ) : UpdateError(message, technicalDetails, requestUrl = requestUrl, canRetry = true)
}

/**
 * Clean update state model representing all phases of update checking, downloading, validation, and installation.
 * Avoids scattered contradictory boolean states.
 */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val currentVersionName: String, val currentVersionCode: Long) : UpdateState
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState
    data class Downloading(
        val info: UpdateInfo,
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val speedBytesPerSec: Long = 0L
    ) : UpdateState
    data class DownloadComplete(val info: UpdateInfo, val apkPath: String) : UpdateState
    data class WaitingForInstallPermission(val info: UpdateInfo, val apkPath: String) : UpdateState
    data class Installing(val info: UpdateInfo, val apkPath: String) : UpdateState
    data class CheckError(val error: UpdateError) : UpdateState
    data class DownloadError(val message: String, val canRetry: Boolean = true) : UpdateState
    data class ValidationError(val message: String) : UpdateState
}

data class UpdateAsset(
    val name: String,
    val size: Long,
    val downloadUrl: String,
    val isApk: Boolean,
    val isSha256Checksum: Boolean
)

data class UpdateInfo(
    val releaseId: Long,
    val tagName: String,
    val releaseName: String,
    val releaseNotes: String,
    val publishedAt: String,
    val releaseUrl: String,
    val apkAsset: UpdateAsset,
    val checksumAsset: UpdateAsset? = null,
    val remoteVersionCode: Long,
    val remoteVersionName: String,
    val isDowngradeOrSame: Boolean = false
)
