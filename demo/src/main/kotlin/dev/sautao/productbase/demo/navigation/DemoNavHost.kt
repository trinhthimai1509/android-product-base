package dev.sautao.productbase.demo.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import dev.sautao.productbase.core.common.ThemeMode
import dev.sautao.productbase.demo.DemoNotifications
import dev.sautao.productbase.demo.R
import dev.sautao.productbase.demo.ui.BackButton
import dev.sautao.productbase.demo.ui.catalog.CatalogScreen
import dev.sautao.productbase.demo.ui.data.DataScreen
import dev.sautao.productbase.demo.ui.data.DataViewModel
import dev.sautao.productbase.demo.ui.home.HomeScreen
import dev.sautao.productbase.demo.ui.infra.InfraScreen
import dev.sautao.productbase.demo.ui.infra.InfraViewModel
import dev.sautao.productbase.demo.ui.monetisation.MonetisationScreen
import dev.sautao.productbase.demo.ui.monetisation.MonetisationViewModel
import dev.sautao.productbase.demo.ui.network.NetworkScreen
import dev.sautao.productbase.demo.ui.network.NetworkViewModel
import dev.sautao.productbase.demo.ui.onboarding.demoOnboardingPages
import dev.sautao.productbase.demo.ui.settings.DemoSettingsScreen
import dev.sautao.productbase.demo.ui.settings.DemoSettingsViewModel
import dev.sautao.productbase.feature.onboarding.OnboardingScreen
import dev.sautao.productbase.feature.onboarding.OnboardingViewModel
import dev.sautao.productbase.feature.premium.PremiumContent
import dev.sautao.productbase.feature.premium.PremiumScreen
import dev.sautao.productbase.feature.premium.PremiumViewModel
import kotlinx.serialization.Serializable

/**
 * Type-safe routes. Destinations are serializable objects rather than strings, so a typo is a
 * compile error instead of a runtime crash.
 */
@Serializable
data object OnboardingRoute

@Serializable
data object HomeRoute

@Serializable
data object CatalogRoute

@Serializable
data object SettingsRoute

@Serializable
data object DataRoute

@Serializable
data object NetworkRoute

@Serializable
data object InfraRoute

@Serializable
data object MonetisationRoute

@Serializable
data object PremiumRoute

@Composable
fun DemoNavHost(
    onboardingCompleted: Boolean,
    themeMode: ThemeMode,
    dynamicColorEnabled: Boolean,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    val navController = rememberNavController()
    val navigateBack: () -> Unit = { navController.popBackStack() }

    // Read once. Changing a NavHost's start destination later rebuilds the graph and throws the
    // back stack away, so where the app *starts* is decided before the first frame and every
    // later change is a navigation instead.
    val startDestination: Any = remember { if (onboardingCompleted) HomeRoute else OnboardingRoute }

    val currentEntry by navController.currentBackStackEntryAsState()
    val onOnboarding = currentEntry?.destination?.hasRoute(OnboardingRoute::class) == true

    // The demo's "replay onboarding" affordance: clearing the flag is what sends the user back,
    // and it works from anywhere because the flag — not a button — is the source of truth.
    LaunchedEffect(onboardingCompleted, onOnboarding) {
        if (!onboardingCompleted && !onOnboarding) navController.navigate(OnboardingRoute)
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable<OnboardingRoute> {
            val viewModel: OnboardingViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // Navigation follows the persisted flag, not the button press: if the write failed,
            // the user stays where they are rather than landing in an app that will show them
            // onboarding again tomorrow.
            LaunchedEffect(uiState.isCompleted) {
                if (uiState.isCompleted) {
                    navController.navigate(HomeRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                }
            }

            OnboardingScreen(
                // Supplied by this product. The base ships no pages and no copy.
                pages = demoOnboardingPages(),
                uiState = uiState,
                onNext = viewModel::next,
                onSkip = viewModel::skip,
                onFinish = viewModel::finish,
                onPageChanged = viewModel::goToPage,
            )
        }
        composable<HomeRoute> {
            HomeScreen(
                onOpenCatalog = { navController.navigate(CatalogRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenData = { navController.navigate(DataRoute) },
                onOpenNetwork = { navController.navigate(NetworkRoute) },
                onOpenInfra = { navController.navigate(InfraRoute) },
                onOpenMonetisation = { navController.navigate(MonetisationRoute) },
            )
        }
        composable<CatalogRoute> {
            CatalogScreen(onNavigateBack = navigateBack)
        }
        composable<SettingsRoute> {
            val activity = LocalActivity.current
            val viewModel: DemoSettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            DemoSettingsScreen(
                uiState = uiState,
                // Theme lives on the Activity because the theme wraps the whole app; settings
                // only reports the user's choice back to it.
                themeMode = themeMode,
                dynamicColorEnabled = dynamicColorEnabled,
                onThemeModeChange = onThemeModeChange,
                onDynamicColorChange = onDynamicColorChange,
                onNotificationsChange = viewModel::setNotificationsEnabled,
                onShowPrivacyOptions = { activity?.let(viewModel::showPrivacyOptions) },
                // Cross-feature navigation is the product's job. There is no
                // feature:settings -> feature:premium dependency, and this lambda is why.
                onOpenPremium = { navController.navigate(PremiumRoute) },
                onRestorePurchase = viewModel::restorePurchases,
                onRateApp = { activity?.let(viewModel::requestReview) },
                onReplayOnboarding = viewModel::resetOnboarding,
                onActionUnhandled = viewModel::onActionUnhandled,
                onDismissMessage = viewModel::dismissMessage,
                onNavigateBack = navigateBack,
            )
        }
        composable<DataRoute> {
            val viewModel: DataViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            DataScreen(
                uiState = uiState,
                onAddEntry = viewModel::addEntry,
                onClearEntries = viewModel::clearEntries,
                onNavigateBack = navigateBack,
            )
        }
        composable<NetworkRoute> {
            val viewModel: NetworkViewModel = hiltViewModel()
            val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
            val requestState by viewModel.requestState.collectAsStateWithLifecycle()
            NetworkScreen(
                isOnline = isOnline,
                requestState = requestState,
                onSendRequest = viewModel::sendRequest,
                onNavigateBack = navigateBack,
            )
        }
        composable<MonetisationRoute> {
            val activity = LocalActivity.current
            val viewModel: MonetisationViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MonetisationScreen(
                uiState = uiState,
                adsController = viewModel.adsController,
                onRecordAction = viewModel::recordQualifyingAction,
                onShowInterstitial = { activity?.let(viewModel::showInterstitial) },
                onShowRewarded = { activity?.let(viewModel::showRewarded) },
                onShowPrivacyOptions = { activity?.let(viewModel::showPrivacyOptions) },
                onOpenPremium = {
                    viewModel.onPaywallViewed()
                    navController.navigate(PremiumRoute)
                },
                onNavigateBack = navigateBack,
            )
        }
        composable<PremiumRoute> {
            val activity = LocalActivity.current
            val viewModel: PremiumViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            PremiumScreen(
                // Supplied by this product. The base ships no benefits and no marketing copy.
                content = PremiumContent(
                    title = stringResource(R.string.premium_title),
                    subtitle = stringResource(R.string.premium_subtitle),
                    benefits = listOf(
                        stringResource(R.string.premium_benefit_ads),
                        stringResource(R.string.premium_benefit_support),
                        stringResource(R.string.premium_benefit_lifetime),
                    ),
                    footnote = stringResource(R.string.premium_footnote),
                ),
                uiState = uiState,
                onPurchase = { activity?.let(viewModel::purchase) },
                onRestore = viewModel::restore,
                onDismissMessage = viewModel::dismissMessage,
                navigationIcon = { BackButton(navigateBack) },
            )
        }
        composable<InfraRoute>(
            // Tapping a scheduled reminder lands here.
            deepLinks = listOf(navDeepLink { uriPattern = DemoNotifications.REMINDER_DEEP_LINK }),
        ) {
            val viewModel: InfraViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            InfraScreen(
                uiState = uiState,
                onSendTelemetry = viewModel::sendTelemetry,
                onScheduleReminder = viewModel::scheduleReminder,
                onCancelReminder = viewModel::cancelReminder,
                onNavigateBack = navigateBack,
            )
        }
    }
}
