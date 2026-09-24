package com.Crescent.DhikrCounter.core.update.model

/**
 * Clean error representation for update operations.
 */
sealed class UpdateError(
    open val message: String,
    open val canRetry: Boolean = true
) {
    data class NetworkError(
        override val message: String = "Couldn't reach GitHub right now. Please check your internet connection and try again."
    ) : UpdateError(message, canRetry = true)

    data class ReleaseVersionError(
        override val message: String = "Unable to determine the GitHub release version."
    ) : UpdateError(message, canRetry = true)

    data class ApkNotFound(
        val tagName: String,
        override val message: String = "No compatible APK found in the latest release."
    ) : UpdateError(message, canRetry = true)

    data class Generic(
        override val message: String
    ) : UpdateError(message, canRetry = true)
}

/**
 * Clean update state model representing all phases of update checking, downloading, validation, and installation.
 */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val currentVersionName: String) : UpdateState
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
    val isApk: Boolean
)

data class UpdateInfo(
    val releaseId: Long,
    val tagName: String,
    val releaseName: String,
    val releaseNotes: String,
    val publishedAt: String,
    val releaseUrl: String,
    val apkAsset: UpdateAsset,
    val remoteVersionName: String
)

