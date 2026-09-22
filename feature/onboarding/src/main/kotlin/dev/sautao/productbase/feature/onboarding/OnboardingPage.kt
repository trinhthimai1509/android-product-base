package dev.sautao.productbase.feature.onboarding

import androidx.compose.runtime.Composable

/**
 * One page of the product's onboarding.
 *
 * The base ships no pages and no copy. What an app says about itself in the first thirty seconds
 * is the product's most important writing, and a base that supplied it would produce twenty apps
 * that introduce themselves identically.
 *
 * @param title already-localised, resolved by the product from its own resources.
 * @param description already-localised. One idea per page; users skip walls of text.
 * @param illustration optional artwork, animation or anything else Composable. A slot rather than
 * a drawable resource, so a product can use a vector, a Lottie animation or nothing at all
 * without this module knowing what any of those are.
 */
data class OnboardingPage(
    val title: String,
    val description: String,
    val illustration: (@Composable () -> Unit)? = null,
)
