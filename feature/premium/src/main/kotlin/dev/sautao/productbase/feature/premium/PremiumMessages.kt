package dev.sautao.productbase.feature.premium

import dev.sautao.productbase.core.billing.PurchaseOutcome
import dev.sautao.productbase.core.billing.PurchaseState

/**
 * What to tell the user after an action, as pure functions.
 *
 * Separated from the ViewModel because launching a purchase needs a real `Activity`, which a JVM
 * test cannot create — so the decisions live here, where they are testable, and the ViewModel is
 * left with plumbing.
 */
internal fun PurchaseOutcome.toMessage(): PremiumMessage? = when (this) {
    PurchaseOutcome.Purchased -> PremiumMessage.PurchaseCompleted

    PurchaseOutcome.Pending -> PremiumMessage.PurchasePending

    PurchaseOutcome.AlreadyOwned -> PremiumMessage.AlreadyOwned

    // Backing out is a decision, not a failure: no dialog.
    PurchaseOutcome.Cancelled -> null

    is PurchaseOutcome.Failed -> PremiumMessage.Failed(error)
}

/** What a restore found. */
internal fun PurchaseState.toRestoreMessage(): PremiumMessage = when (this) {
    PurchaseState.Purchased -> PremiumMessage.PurchaseCompleted

    PurchaseState.Pending -> PremiumMessage.PurchasePending

    PurchaseState.NotPurchased -> PremiumMessage.NothingToRestore

    // "We could not ask" is not "you own nothing".
    PurchaseState.Unknown -> PremiumMessage.CouldNotCheck
}
