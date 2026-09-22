package dev.sautao.productbase.demo.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.sautao.productbase.core.designsystem.theme.AppTheme
import dev.sautao.productbase.demo.R
import dev.sautao.productbase.feature.onboarding.OnboardingPage

private val ILLUSTRATION_SIZE = 120.dp

/**
 * The demo's onboarding content.
 *
 * All of it lives here, in the product, because that is the point: `feature:onboarding` supplies
 * paging, skipping, finishing and persistence, and knows none of these words or pictures.
 */
@Composable
internal fun demoOnboardingPages(): List<OnboardingPage> = listOf(
    OnboardingPage(
        title = stringResource(R.string.onboarding_capabilities_title),
        description = stringResource(R.string.onboarding_capabilities_description),
        illustration = { Illustration(Icons.Filled.Star) },
    ),
    OnboardingPage(
        title = stringResource(R.string.onboarding_settings_title),
        description = stringResource(R.string.onboarding_settings_description),
        illustration = { Illustration(Icons.Filled.Settings) },
    ),
    OnboardingPage(
        title = stringResource(R.string.onboarding_privacy_title),
        description = stringResource(R.string.onboarding_privacy_description),
        illustration = { Illustration(Icons.Filled.Lock) },
    ),
)

/**
 * Decorative: the page title and description carry the meaning, so a screen reader that announced
 * the icon as well would just repeat itself.
 */
@Composable
private fun Illustration(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(ILLUSTRATION_SIZE)
            .background(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(AppTheme.spacing.lg),
        )
    }
}
