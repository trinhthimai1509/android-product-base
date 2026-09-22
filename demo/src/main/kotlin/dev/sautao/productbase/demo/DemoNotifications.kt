package dev.sautao.productbase.demo

/**
 * The demo's notification identifiers.
 *
 * Channel ids and deep links are product-owned: `core:notification` carries whatever it is
 * given and defines none of its own.
 */
object DemoNotifications {
    const val REMINDER_CHANNEL_ID = "demo_reminders"

    /** Matches the intent-filter on MainActivity and the deep link on the infrastructure route. */
    const val REMINDER_DEEP_LINK = "productbase-demo://infrastructure"
}
