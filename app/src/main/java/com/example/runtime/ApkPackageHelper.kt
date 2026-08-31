package com.example.runtime

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

data class ParsedApkInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val appName: String,
    val minSdk: Int,
    val targetSdk: Int,
    val isInstalled: Boolean,
    val launchIntent: Intent?
)

object ApkPackageHelper {

    /**
     * Parses an APK archive file using Android PackageManager to retrieve real package metadata.
     */
    fun parseApk(context: Context, apkFile: File): ParsedApkInfo? {
        if (!apkFile.exists() || !apkFile.name.endsWith(".apk", ignoreCase = true)) return null
        return try {
            val pm = context.packageManager
            val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA
            val packageInfo: PackageInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            }

            if (packageInfo != null) {
                val appInfo = packageInfo.applicationInfo ?: return null
                appInfo.sourceDir = apkFile.absolutePath
                appInfo.publicSourceDir = apkFile.absolutePath

                val appName = try {
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    packageInfo.packageName
                }

                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }

                val isInstalled = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getPackageInfo(packageInfo.packageName, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(packageInfo.packageName, 0)
                    }
                    true
                } catch (e: Exception) {
                    false
                }

                val launchIntent = if (isInstalled) {
                    pm.getLaunchIntentForPackage(packageInfo.packageName)
                } else null

                val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appInfo.minSdkVersion
                } else {
                    0
                }

                ParsedApkInfo(
                    packageName = packageInfo.packageName,
                    versionName = packageInfo.versionName ?: "1.0",
                    versionCode = versionCode,
                    appName = appName,
                    minSdk = minSdk,
                    targetSdk = appInfo.targetSdkVersion,
                    isInstalled = isInstalled,
                    launchIntent = launchIntent
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates an Intent to install the given APK file via Android OS PackageInstaller.
     */
    fun createInstallIntent(context: Context, apkFile: File): Intent {
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
        } catch (e: Exception) {
            Uri.fromFile(apkFile)
        }
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    /**
     * Checks if a package is currently installed on the device.
     */
    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Retrieves the launch intent for an installed package.
     */
    fun getLaunchIntent(context: Context, packageName: String): Intent? {
        return context.packageManager.getLaunchIntentForPackage(packageName)
    }
}
