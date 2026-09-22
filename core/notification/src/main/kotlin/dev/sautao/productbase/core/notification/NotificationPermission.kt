package dev.sautao.productbase.core.notification

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * The permission to request on Android 13+. Products pass this to their own permission launcher.
 *
 * The constant is inlined by the compiler and is only ever a string, so referencing it below
 * API 33 is safe; it is the *request* that has to be version-guarded, and callers do that.
 */
@SuppressLint("InlinedApi")
const val POST_NOTIFICATIONS_PERMISSION: String = Manifest.permission.POST_NOTIFICATIONS

/**
 * Whether a notification posted right now would be shown.
 *
 * Covers both halves of the problem: the runtime permission on Android 13+, and the user having
 * turned notifications off in system settings, which is possible on every version and which the
 * permission check alone would miss.
 *
 * No Compose helper is provided on purpose — requesting a permission belongs to the product's UI
 * layer, and this module stays free of a UI dependency.
 */
fun Context.canPostNotifications(): Boolean {
    val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    return permissionGranted && NotificationManagerCompat.from(this).areNotificationsEnabled()
}
