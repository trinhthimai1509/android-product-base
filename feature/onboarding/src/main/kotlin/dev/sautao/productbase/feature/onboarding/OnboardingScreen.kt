package dev.sautao.productbase.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.sautao.productbase.core.designsystem.component.AppScaffold
import dev.sautao.productbase.core.designsystem.component.PrimaryButton
import dev.sautao.productbase.core.designsystem.theme.AppTheme

/**
 * The onboarding mechanism: pages, progression, skip and finish.
 *
 * Stateless — state comes in as [uiState] and intent goes out as callbacks — so it can be
 * previewed, screenshot-tested and driven by something other than [OnboardingViewModel].
 *
 * @param pages supplied by the product. Nothing is shown if the list is empty.
 * @param onNext receives the page count so the caller does not have to track it separately.
 * @param onPageChanged reports a swipe, so the pager and the observable state agree.
 */
@Composable
fun OnboardingScreen(
    pages: List<OnboardingPage>,
    uiState: OnboardingUiState,
    onNext: (pageCount: Int) -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pages.isEmpty()) return

    val pageIndex = uiState.pageIndex.coerceIn(0, pages.lastIndex)
    val isLastPage = pageIndex == pages.lastIndex
    val pagerState = rememberPagerState(initialPage = pageIndex) { pages.size }

    // Two directions, one source of truth: a swipe reports upwards, and a state change that did
    // not come from a swipe scrolls the pager. Each guard makes the other a no-op.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect(onPageChanged)
    }
    LaunchedEffect(pageIndex) {
        if (pagerState.currentPage != pageIndex) pagerState.animateScrollToPage(pageIndex)
    }

    AppScaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                // Kept on every page including the last, so its position never jumps.
                TextButton(onClick = onSkip, enabled = !isLastPage) {
                    Text(text = stringResource(R.string.pb_onboarding_skip))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            PageIndicator(
                pageIndex = pageIndex,
                pageCount = pages.size,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            PrimaryButton(
                text = stringResource(
                    if (isLastPage) R.string.pb_onboarding_finish else R.string.pb_onboarding_next,
                ),
                onClick = { if (isLastPage) onFinish() else onNext(pages.size) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppTheme.spacing.md),
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        page.illustration?.invoke()
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = AppTheme.spacing.lg),
        )
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = AppTheme.spacing.sm),
        )
    }
}

/**
 * Dots, announced as one piece of text.
 *
 * A screen reader that reads five anonymous dots tells the user nothing; "page 2 of 5" tells
 * them exactly what the dots tell everyone else.
 */
@Composable
private fun PageIndicator(pageIndex: Int, pageCount: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.pb_onboarding_page_indicator, pageIndex + 1, pageCount)

    Row(
        modifier = modifier
            .padding(AppTheme.spacing.sm)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(AppTheme.spacing.sm)
                    .clearAndSetSemantics {}
                    .background(
                        color = if (index == pageIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}
