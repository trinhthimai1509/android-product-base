package dev.sautao.productbase.feature.premium

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.sautao.productbase.core.billing.BillingError
import dev.sautao.productbase.core.billing.PurchaseState
import dev.sautao.productbase.core.designsystem.component.AppCard
import dev.sautao.productbase.core.designsystem.component.AppDialog
import dev.sautao.productbase.core.designsystem.component.AppScaffold
import dev.sautao.productbase.core.designsystem.component.AppTopBar
import dev.sautao.productbase.core.designsystem.component.PremiumBadge
import dev.sautao.productbase.core.designsystem.component.PrimaryButton
import dev.sautao.productbase.core.designsystem.component.SecondaryButton
import dev.sautao.productbase.core.designsystem.theme.AppTheme

/**
 * A paywall mechanism, not a paywall.
 *
 * Stateless: the product supplies [content], the caller supplies state and callbacks. Nothing
 * here knows the app's name, its benefits or its price — the price comes from Google Play,
 * already localised, and everything else comes from [PremiumContent].
 */
@Composable
fun PremiumScreen(
    content: PremiumContent,
    uiState: PremiumUiState,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
) {
    AppScaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = content.title, navigationIcon = navigationIcon) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(AppTheme.spacing.md)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(text = content.subtitle, style = MaterialTheme.typography.bodyLarge)

            AppCard(modifier = Modifier.padding(top = AppTheme.spacing.md)) {
                content.benefits.forEach { benefit ->
                    Text(
                        text = benefit,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = AppTheme.spacing.xs),
                    )
                }
            }

            when (uiState.purchaseState) {
                PurchaseState.Purchased -> OwnedState()

                PurchaseState.Pending -> Note(stringResource(R.string.pb_premium_pending))

                PurchaseState.NotPurchased, PurchaseState.Unknown -> PurchaseActions(
                    uiState = uiState,
                    onPurchase = onPurchase,
                    onRestore = onRestore,
                )
            }

            content.footnote?.let { Note(it) }
        }
    }

    uiState.message?.let { message ->
        AppDialog(
            title = content.title,
            text = message.describe(),
            confirmText = stringResource(R.string.pb_premium_dismiss),
            onConfirm = onDismissMessage,
            onDismiss = onDismissMessage,
        )
    }
}

@Composable
private fun OwnedState() {
    Column(modifier = Modifier.padding(top = AppTheme.spacing.lg)) {
        PremiumBadge(text = stringResource(R.string.pb_premium_badge))
        Note(stringResource(R.string.pb_premium_owned))
    }
}

@Composable
private fun PurchaseActions(
    uiState: PremiumUiState,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
) {
    val product = uiState.product
    val buyLabel = when {
        product != null -> stringResource(R.string.pb_premium_buy_with_price, product.formattedPrice)
        else -> stringResource(R.string.pb_premium_buy)
    }

    PrimaryButton(
        text = buyLabel,
        onClick = onPurchase,
        // Nothing to buy until Play has supplied a price: a purchase button that cannot work is
        // worse than one that is visibly waiting.
        enabled = product != null && !uiState.isWorking,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.spacing.lg),
    )
    SecondaryButton(
        text = stringResource(R.string.pb_premium_restore),
        onClick = onRestore,
        enabled = !uiState.isWorking,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = AppTheme.spacing.sm),
    )

    if (product == null) {
        Note(
            if (uiState.purchaseState == PurchaseState.Unknown) {
                stringResource(R.string.pb_premium_loading_price)
            } else {
                stringResource(R.string.pb_premium_unavailable)
            },
        )
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = AppTheme.spacing.sm),
    )
}

/** Billing outcomes become text here, at the UI edge — the ViewModel never touches a resource. */
@Composable
private fun PremiumMessage.describe(): String = when (this) {
    PremiumMessage.PurchaseCompleted -> stringResource(R.string.pb_premium_message_completed)

    PremiumMessage.PurchasePending -> stringResource(R.string.pb_premium_message_pending)

    PremiumMessage.AlreadyOwned -> stringResource(R.string.pb_premium_message_already_owned)

    PremiumMessage.NothingToRestore -> stringResource(R.string.pb_premium_message_nothing_to_restore)

    PremiumMessage.CouldNotCheck -> stringResource(R.string.pb_premium_message_could_not_check)

    is PremiumMessage.Failed -> when (error) {
        BillingError.BillingUnavailable -> stringResource(R.string.pb_premium_message_error_unavailable)

        BillingError.ServiceDisconnected -> stringResource(R.string.pb_premium_message_error_disconnected)

        BillingError.NetworkUnavailable -> stringResource(R.string.pb_premium_message_error_network)

        BillingError.ProductUnavailable -> stringResource(R.string.pb_premium_message_error_product)

        // Never shown raw: a developer error is this app's bug, and the user can only be told
        // that something went wrong.
        BillingError.DeveloperError, is BillingError.Unknown ->
            stringResource(R.string.pb_premium_message_error_generic)
    }
}
