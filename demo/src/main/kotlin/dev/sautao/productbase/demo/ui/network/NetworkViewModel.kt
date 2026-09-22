package dev.sautao.productbase.demo.ui.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sautao.productbase.core.network.NetworkError
import dev.sautao.productbase.core.network.NetworkMonitor
import dev.sautao.productbase.core.network.toNetworkError
import dev.sautao.productbase.demo.data.SampleApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The result of the sample request.
 *
 * [Failure] carries the `NetworkError` itself rather than a message: mapping it to text is the
 * screen's job, which keeps this class free of string resources and testable without a Context.
 */
sealed interface RequestState {
    data object Idle : RequestState

    data object InFlight : RequestState

    data class Success(val url: String) : RequestState

    data class Failure(val error: NetworkError) : RequestState
}

@HiltViewModel
class NetworkViewModel @Inject constructor(private val api: SampleApi, networkMonitor: NetworkMonitor) : ViewModel() {
    val isOnline: StateFlow<Boolean?> = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            // null until the first reading arrives, so the UI can say "checking" instead of
            // claiming the device is offline for a frame.
            initialValue = null,
        )

    private val _requestState = MutableStateFlow<RequestState>(RequestState.Idle)
    val requestState: StateFlow<RequestState> = _requestState.asStateFlow()

    fun sendRequest() {
        if (_requestState.value == RequestState.InFlight) return
        _requestState.value = RequestState.InFlight
        viewModelScope.launch {
            _requestState.value = runCatching { api.echo() }.fold(
                onSuccess = { RequestState.Success(it.url) },
                // Mapped, never rethrown and never shown raw: toNetworkError() turns whatever
                // OkHttp, Retrofit or the parser threw into something a screen can explain.
                onFailure = { RequestState.Failure(it.toNetworkError()) },
            )
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
