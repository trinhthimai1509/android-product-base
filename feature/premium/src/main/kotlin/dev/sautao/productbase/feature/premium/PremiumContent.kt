package dev.sautao.productbase.feature.premium

/**
 * Everything a product must say for itself.
 *
 * The base ships no title, no benefits and no marketing copy — a paywall that reads the same in
 * twenty apps is exactly the kind of reuse this repository is supposed to prevent. All of these
 * are already-localised strings the product resolves from its own resources.
 *
 * @param benefits shown in order. What the product actually offers, in the product's words.
 * @param footnote optional; a good place for "one-time payment, no subscription" where true.
 */
data class PremiumContent(
    val title: String,
    val subtitle: String,
    val benefits: List<String>,
    val footnote: String? = null,
)
