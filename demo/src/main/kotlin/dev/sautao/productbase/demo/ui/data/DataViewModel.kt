package dev.sautao.productbase.demo.ui.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sautao.productbase.demo.data.SampleEntryDao
import dev.sautao.productbase.demo.data.SampleEntryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class DataUiState(val isLoading: Boolean = true, val entries: List<SampleEntryEntity> = emptyList())

/**
 * Demonstrates Room through the DAO directly.
 *
 * There is no repository here on purpose: a class that forwarded three DAO calls unchanged would
 * add a file and hide nothing.
 */
@HiltViewModel
class DataViewModel @Inject constructor(private val dao: SampleEntryDao) : ViewModel() {
    val uiState: StateFlow<DataUiState> = dao.observeAll()
        .map { DataUiState(isLoading = false, entries = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DataUiState(),
        )

    fun addEntry() {
        viewModelScope.launch {
            val count = uiState.value.entries.size + 1
            dao.insert(SampleEntryEntity(label = "Entry $count", createdAt = Instant.now()))
        }
    }

    fun clearEntries() {
        viewModelScope.launch { dao.deleteAll() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
