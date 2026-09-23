package com.nj031.onetask.data.blocking

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/** Known package names for the Distracting Apps shortlist (Phase 5 spec's fixed initial list) -
 * looked up against what's actually installed via [findInstalledApp], never shown for a device
 * that doesn't have them (per explicit product decision: no bundled placeholder icons for an app
 * that isn't there). */
val DISTRACTING_APP_PACKAGES = listOf(
    "com.instagram.android",
    "com.whatsapp",
    "com.facebook.katana",
    "com.zhiliaoapp.musically",
    "com.netflix.mediaclient",
    "com.spotify.music",
    "com.reddit.frontpage",
    "com.snapchat.android",
    "com.amazon.mShop.android.shopping"
)

const val DISCORD_PACKAGE = "com.discord"
const val TELEGRAM_PACKAGE = "org.telegram.messenger"

/** One selectable app in the Block Distractions picker - just enough to render a row (icon/name)
 * and identify it (packageName), never a package name shown to the user. */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

/** True for a pure system app the user never "installed" in any meaningful sense (was never
 * updated as a regular app) - Android's own Settings > Apps "show system apps" toggle uses this
 * same flag combination, which is why it's used here to hide system/internal apps by default. */
private fun isSystemApp(appInfo: ApplicationInfo): Boolean =
    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) &&
        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0)

/** Looks up one specific app by its known package name - null if it isn't installed on this
 * device. Used for the fixed Distracting Apps shortlist and Discord/Telegram, which are looked up
 * directly rather than filtered out of [queryLaunchableUserApps] (a shortlist app not currently
 * resolving as a launcher activity for some reason should still count as "installed" here). */
fun findInstalledApp(context: Context, packageName: String): InstalledApp? {
    val pm = context.packageManager
    return try {
        val appInfo = pm.getApplicationInfo(packageName, 0)
        InstalledApp(
            packageName = packageName,
            label = pm.getApplicationLabel(appInfo).toString(),
            icon = pm.getApplicationIcon(appInfo)
        )
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}

/** Every other launchable user app on the device (icon + name only, no package name shown) -
 * excludes this app itself and, via [excludedPackages], whatever the caller already lists
 * explicitly elsewhere (the Distracting Apps shortlist, Discord, Telegram) so no app is ever
 * duplicated across sections. System/internal apps are hidden by default (see [isSystemApp]).
 *
 * Queried with flags = 0, not MATCH_DEFAULT_ONLY: that flag restricts results to activities whose
 * intent-filter also declares CATEGORY_DEFAULT, which is the right flag for resolving "what should
 * handle this generic Intent by default" but wrong here - a launcher activity only needs MAIN +
 * LAUNCHER (this app's own MainActivity declares exactly that, no CATEGORY_DEFAULT, same as many
 * other apps' launcher activities), so MATCH_DEFAULT_ONLY was silently dropping otherwise-eligible,
 * launcher-visible installed apps from this list. This is the same flag-less pattern a standard
 * Android app drawer/launcher uses to enumerate all launchable apps. */
fun queryLaunchableUserApps(context: Context, excludedPackages: Set<String>): List<InstalledApp> {
    val pm = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val ownPackage = context.packageName
    val fullyExcluded = excludedPackages + ownPackage

    return pm.queryIntentActivities(launcherIntent, 0)
        .distinctBy { it.activityInfo.packageName }
        .asSequence()
        .filter { info -> info.activityInfo.packageName !in fullyExcluded }
        .filter { info -> !isSystemApp(info.activityInfo.applicationInfo) }
        .map { info ->
            InstalledApp(
                packageName = info.activityInfo.packageName,
                label = info.loadLabel(pm).toString(),
                icon = info.loadIcon(pm)
            )
        }
        .sortedBy { it.label.lowercase() }
        .toList()
}
