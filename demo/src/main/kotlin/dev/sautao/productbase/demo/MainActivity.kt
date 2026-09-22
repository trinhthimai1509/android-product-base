package dev.sautao.productbase.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.sautao.productbase.core.ads.AdsController
import dev.sautao.productbase.core.designsystem.theme.ProductBaseTheme
import dev.sautao.productbase.demo.navigation.DemoNavHost
import dev.sautao.productbase.demo.ui.AppViewModel
import javax.inject.Inject

/**
 * The single Activity. Products keep it this thin: it hosts the theme and the navigation graph
 * and owns no screen logic of its own.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /**
     * Consent and SDK initialisation belong to the first Activity, not to the screen that happens
     * to show an ad: UMP has to have answered before anything is requested, and settings needs
     * to know whether this user's region requires a privacy-options entry.
     */
    @Inject
    lateinit var adsController: AdsController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: AppViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) { adsController.initialize(this@MainActivity) }

            ProductBaseTheme(
                themeMode = uiState.themeMode,
                dynamicColor = uiState.dynamicColorEnabled,
            ) {
                // Nothing is drawn until preferences have been read. One blank frame is the price
                // of never showing a returning user the onboarding they already finished, and of
                // never flashing the light theme at someone who chose dark.
                if (uiState.isLoading) return@ProductBaseTheme

                DemoNavHost(
                    onboardingCompleted = uiState.onboardingCompleted,
                    themeMode = uiState.themeMode,
                    dynamicColorEnabled = uiState.dynamicColorEnabled,
                    onThemeModeChange = viewModel::setThemeMode,
                    onDynamicColorChange = viewModel::setDynamicColorEnabled,
                )
            }
        }
    }
}
