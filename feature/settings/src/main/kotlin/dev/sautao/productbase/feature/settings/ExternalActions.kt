package dev.sautao.productbase.feature.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

// The two things settings rows actually do outside the app: share something, or open a link.
//
// These are utilities, not an abstraction — plain functions over Intent, with no interface, no
// injection and no module of their own. A core:share module was considered and rejected: there is
// no state, no SDK to contain and no second implementation, so a module boundary would buy
// nothing. They live here because the settings screen is their only consumer today; if a second
// one appears somewhere else, they move (ARCHITECTURE_PLAN.md §1.1 rule 3).

/**
 * Offers [text] to whatever app the user picks.
 *
 * The product supplies the text, including its store link — the base ships no listing URL and no
 * marketing sentence, because a shared message that reads identically from twenty apps is spam.
 *
 * @param chooserTitle already-localised; ignored on modern Android, which draws its own sheet.
 * @return false if the device has nothing that can share text, so the caller can stay quiet
 * instead of appearing to do nothing.
 */
fun Context.shareText(text: String, chooserTitle: String? = null): Boolean {
    val send = Intent(Intent.ACTION_SEND).apply {
        // Explicitly text/plain: without a type the chooser offers nothing, and a wider type
        // invites apps that cannot handle a string.
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    return startSafely(Intent.createChooser(send, chooserTitle))
}

/**
 * Opens a web link — a privacy policy, terms, a store listing.
 *
 * Only `http` and `https` are allowed. A settings screen opens URLs that come from product
 * configuration, and configuration is exactly the kind of thing that gets edited by someone in a
 * hurry; refusing every other scheme means a mistake there cannot turn into an `intent://` or
 * `file://` launch.
 *
 * @return false if the URL was refused or nothing on the device can open it.
 */
fun Context.openUrl(url: String): Boolean {
    val uri = url.toUri()
    if (uri.scheme?.lowercase() !in ALLOWED_URL_SCHEMES) return false

    return startSafely(Intent(Intent.ACTION_VIEW, uri))
}

private val ALLOWED_URL_SCHEMES = setOf("http", "https")

private fun Context.startSafely(intent: Intent): Boolean {
    // A non-Activity context has no task to start into, and Android refuses the launch without
    // this flag. Products often call these from an Application or a Service context.
    if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    return try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        // Nothing installed that can handle it — no browser, or nothing that accepts text. The
        // caller gets false and decides what to say; crashing over a share sheet is absurd.
        false
    }
}
