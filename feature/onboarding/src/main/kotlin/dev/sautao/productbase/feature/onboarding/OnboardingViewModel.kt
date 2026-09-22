package dev.sautao.productbase.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sautao.productbase.core.datastore.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Where the user is, and whether they are done.
 *
 * @param isCompleted read back from storage rather than set optimistically, so a product that
 * navigates on it navigates only once the flag is actually persisted.
 */
data class OnboardingUiState(val pageIndex: Int = 0, val isCompleted: Boolean = false)

/**
 * Onboarding progression and its one piece of persistent state.
 *
 * The mechanism is here; the content is not. This class never sees a page, a title or an image —
 * it counts, and it remembers that the user got to the end.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(private val appPreferences: AppPreferences) : ViewModel() {
    private val pageIndex = MutableStateFlow(0)

    val uiState: StateFlow<OnboardingUiState> = combine(
        pageIndex,
        appPreferences.onboardingCompleted,
    ) { index, completed ->
        OnboardingUiState(pageIndex = index, isCompleted = completed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = OnboardingUiState(),
    )

    /**
     * Advances one page, or completes onboarding if this was the last one.
     *
     * @param pageCount how many pages the product supplied. It is a parameter rather than state
     * because that is what keeps this class content-free: the screen knows the pages, so the
     * screen passes the count, and the mechanism never has to own the content.
     */
    fun next(pageCount: Int) {
        if (pageIndex.value >= pageCount - 1) {
            complete()
        } else {
            pageIndex.value = pageIndex.value + 1
        }
    }

    /** Reports a swipe. The pager owns the gesture; this keeps the observable state in step. */
    fun goToPage(index: Int) {
        pageIndex.value = index.coerceAtLeast(0)
    }

    /**
     * The user asked to get past this.
     *
     * Identical to [finish] as far as storage is concerned — a skipped onboarding is a completed
     * one, and showing it again on the next launch ignores what the user just told you. It is a
     * separate call so that call sites read correctly and a product can log the two differently.
     */
    fun skip() = complete()

    /** The user reached the end. */
    fun finish() = complete()

    private fun complete() {
        viewModelScope.launch { appPreferences.setOnboardingCompleted(true) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
