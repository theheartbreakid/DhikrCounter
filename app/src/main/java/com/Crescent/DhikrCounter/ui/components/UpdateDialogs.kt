package com.Crescent.DhikrCounter.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Crescent.DhikrCounter.core.update.UpdateManager
import com.Crescent.DhikrCounter.core.update.model.UpdateError
import com.Crescent.DhikrCounter.core.update.model.UpdateInfo
import com.Crescent.DhikrCounter.core.update.model.UpdateState
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidButton
import com.Crescent.DhikrCounter.ui.components.catalog.components.LiquidDialog
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun UpdateDialogs(
    updateManager: UpdateManager,
    backdrop: Backdrop,
    onRequirePermissionCheck: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateState by updateManager.updateState.collectAsState()
    val adaptiveColor = LocalPrismalAdaptiveColor.current

    when (val state = updateState) {
        is UpdateState.Checking -> {
            LiquidDialog(
                onDismissRequest = {},
                backdrop = backdrop,
                title = "Checking for Updates",
                message = "Connecting to GitHub Releases...",
                positiveText = "Cancel",
                negativeText = null,
                onPositive = { updateManager.resetState() },
                icon = Icons.Outlined.CloudSync,
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        is UpdateState.UpToDate -> {
            LiquidDialog(
                onDismissRequest = { updateManager.resetState() },
                backdrop = backdrop,
                title = "You're Up to Date",
                message = "Dhikr Counter is on the latest version (${state.currentVersionName}, build ${state.currentVersionCode}).",
                positiveText = "OK",
                negativeText = null,
                onPositive = { updateManager.resetState() },
                icon = Icons.Outlined.CheckCircle,
                iconTint = Color(0xFF10B981)
            )
        }

        is UpdateState.CheckError -> {
            val error = state.error
            var showDetails by remember { mutableStateOf(false) }

            LiquidDialog(
                onDismissRequest = { updateManager.resetState() },
                backdrop = backdrop,
                title = "Unable to Check for Updates",
                positiveText = if (error.canRetry) "Retry" else "OK",
                negativeText = "Dismiss",
                onPositive = {
                    if (error.canRetry) {
                        scope.launch {
                            updateManager.checkForUpdates(isManual = true)
                        }
                    } else {
                        updateManager.resetState()
                    }
                },
                icon = Icons.Outlined.ErrorOutline,
                iconTint = MaterialTheme.colorScheme.error
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = error.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = adaptiveColor
                    )

                    // Diagnostic info section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(adaptiveColor.copy(alpha = 0.05f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Internet:", style = MaterialTheme.typography.labelMedium, color = adaptiveColor.copy(alpha = 0.6f))
                            Text(
                                if (updateManager.isNetworkAvailable()) "Connected" else "Offline",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (updateManager.isNetworkAvailable()) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                        }

                        if (error.httpStatus != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("HTTP Status:", style = MaterialTheme.typography.labelMedium, color = adaptiveColor.copy(alpha = 0.6f))
                                Text("${error.httpStatus}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = adaptiveColor)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GitHub Server:", style = MaterialTheme.typography.labelMedium, color = adaptiveColor.copy(alpha = 0.6f))
                            Text(
                                if (error is UpdateError.DnsError || error is UpdateError.Timeout || error is UpdateError.ConnectionError) "Unreachable" else "Reached",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (error is UpdateError.DnsError || error is UpdateError.Timeout || error is UpdateError.ConnectionError) MaterialTheme.colorScheme.error else Color(0xFF10B981)
                            )
                        }
                    }

                    // Fallback to view release on GitHub in browser
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val url = error.requestUrl ?: UpdateManager.RELEASES_PAGE_URL
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "View Release on GitHub",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Expandable technical details
                    if (!error.technicalDetails.isNullOrBlank()) {
                        Row(
                            modifier = Modifier
                                .clickable { showDetails = !showDetails }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (showDetails) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = adaptiveColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (showDetails) "Hide Technical Details" else "Technical Details",
                                style = MaterialTheme.typography.labelSmall,
                                color = adaptiveColor.copy(alpha = 0.6f)
                            )
                        }

                        AnimatedVisibility(visible = showDetails) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(adaptiveColor.copy(alpha = 0.08f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = error.technicalDetails ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = adaptiveColor.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        is UpdateState.UpdateAvailable -> {
            val info = state.info
            val currentName = updateManager.getCurrentVersionName()
            val apkSizeMb = if (info.apkAsset.size > 0) {
                String.format(Locale.ROOT, "%.1f MB", info.apkAsset.size / (1024f * 1024f))
            } else {
                "Unknown size"
            }

            LiquidDialog(
                onDismissRequest = { updateManager.resetState() },
                backdrop = backdrop,
                title = "Update Available",
                positiveText = "Update Now",
                negativeText = "Later",
                onPositive = {
                    // Check if already downloaded and valid
                    val downloadedPath = updateManager.getDownloadedApkPathIfValid(info)
                    if (downloadedPath != null) {
                        if (updateManager.canRequestPackageInstalls()) {
                            updateManager.launchInstaller(downloadedPath)
                        } else {
                            updateManager.setWaitingForPermission(info, downloadedPath)
                        }
                    } else {
                        updateManager.startDownload(info, scope)
                    }
                },
                icon = Icons.Outlined.SystemUpdate,
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Dhikr Counter ${info.remoteVersionName} is available.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = adaptiveColor
                    )
                    Text(
                        text = "You are currently using $currentName.",
                        style = MaterialTheme.typography.bodySmall,
                        color = adaptiveColor.copy(alpha = 0.7f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Download size: $apkSizeMb",
                            style = MaterialTheme.typography.labelMedium,
                            color = adaptiveColor.copy(alpha = 0.6f)
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl))
                                    context.startActivity(intent)
                                }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "View Release",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    if (info.releaseNotes.isNotBlank()) {
                        HorizontalDivider(color = adaptiveColor.copy(alpha = 0.1f))
                        Text(
                            text = "What's New:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = adaptiveColor
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(adaptiveColor.copy(alpha = 0.05f))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = sanitizeReleaseNotes(info.releaseNotes),
                                style = MaterialTheme.typography.bodySmall,
                                color = adaptiveColor.copy(alpha = 0.85f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        is UpdateState.Downloading -> {
            val info = state.info
            val downloadedMb = state.bytesDownloaded / (1024f * 1024f)
            val totalMb = state.totalBytes / (1024f * 1024f)
            val speedKb = state.speedBytesPerSec / 1024f

            val speedText = if (speedKb > 1024) {
                String.format(Locale.ROOT, "%.1f MB/s", speedKb / 1024f)
            } else {
                String.format(Locale.ROOT, "%.0f KB/s", speedKb)
            }

            LiquidDialog(
                onDismissRequest = {},
                backdrop = backdrop,
                title = "Downloading Update",
                positiveText = "Cancel",
                negativeText = null,
                onPositive = { updateManager.cancelDownload() },
                icon = Icons.Outlined.CloudDownload,
                iconTint = MaterialTheme.colorScheme.primary
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Downloading Dhikr Counter ${info.remoteVersionName}...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = adaptiveColor
                    )

                    LinearProgressIndicator(
                        progress = { state.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = adaptiveColor.copy(alpha = 0.1f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${state.progressPercent}% (${String.format(Locale.ROOT, "%.1f", downloadedMb)} / ${String.format(Locale.ROOT, "%.1f", totalMb)} MB)",
                            style = MaterialTheme.typography.bodySmall,
                            color = adaptiveColor.copy(alpha = 0.7f)
                        )
                        if (state.speedBytesPerSec > 0) {
                            Text(
                                text = speedText,
                                style = MaterialTheme.typography.bodySmall,
                                color = adaptiveColor.copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        is UpdateState.DownloadComplete -> {
            val info = state.info
            val apkPath = state.apkPath

            LaunchedEffect(apkPath) {
                if (updateManager.canRequestPackageInstalls()) {
                    updateManager.launchInstaller(apkPath)
                } else {
                    updateManager.setWaitingForPermission(info, apkPath)
                }
            }
        }

        is UpdateState.WaitingForInstallPermission -> {
            val apkPath = state.apkPath

            LiquidDialog(
                onDismissRequest = { updateManager.resetState() },
                backdrop = backdrop,
                title = "Allow Installation",
                message = "To install Dhikr Counter updates downloaded directly from GitHub, Android requires you to allow this app to install apps from this source.",
                positiveText = "Open Settings",
                negativeText = "Cancel",
                onPositive = {
                    val intent = updateManager.getManageUnknownAppSourcesIntent()
                    context.startActivity(intent)
                    onRequirePermissionCheck?.invoke()
                },
                icon = Icons.Outlined.Security,
                iconTint = MaterialTheme.colorScheme.primary
            )
        }

        is UpdateState.DownloadError -> {
            LiquidDialog(
                onDismissRequest = { updateManager.resetState() },
                backdrop = backdrop,
                title = "Download Failed",
                message = state.message,
                positiveText = if (state.canRetry) "Retry" else "OK",
                negativeText = if (state.canRetry) "Cancel" else null,
                onPositive = {
                    if (state.canRetry) {
                        scope.launch {
                            updateManager.checkForUpdates(isManual = true)
                        }
                    } else {
                        updateManager.resetState()
                    }
                },
                icon = Icons.Outlined.ErrorOutline,
                iconTint = MaterialTheme.colorScheme.error
            )
        }

        is UpdateState.ValidationError -> {
            LiquidDialog(
                onDismissRequest = { updateManager.resetState() },
                backdrop = backdrop,
                title = "Verification Failed",
                message = state.message,
                positiveText = "Dismiss",
                negativeText = null,
                onPositive = { updateManager.resetState() },
                icon = Icons.Outlined.WarningAmber,
                iconTint = MaterialTheme.colorScheme.error
            )
        }

        else -> {
            // Idle or installing
        }
    }
}

/**
 * Safely strips or cleans GitHub markdown tags to provide readable release notes.
 */
private fun sanitizeReleaseNotes(notes: String): String {
    if (notes.isBlank()) return "Various improvements and bug fixes."

    return notes
        .replace(Regex("""<!--[\s\S]*?-->"""), "") // Remove HTML comments
        .replace(Regex("""<[^>]*>"""), "") // Remove raw HTML tags
        .replace(Regex("""\r\n"""), "\n")
        .trim()
}
