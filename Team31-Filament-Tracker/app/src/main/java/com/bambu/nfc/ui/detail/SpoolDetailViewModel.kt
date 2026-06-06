package com.bambu.nfc.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bambu.nfc.domain.model.Spool
import com.bambu.nfc.domain.model.SpoolStatus
import com.bambu.nfc.domain.repository.SpoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpoolDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SpoolRepository
) : ViewModel() {

    private val spoolId: Long = savedStateHandle.get<Long>("spoolId") ?: 0L

    private val _spool = MutableStateFlow<Spool?>(null)
    val spool: StateFlow<Spool?> = _spool

    private val _navigateBack = MutableStateFlow(false)
    val navigateBack: StateFlow<Boolean> = _navigateBack

    init {
        viewModelScope.launch {
            _spool.value = repository.getById(spoolId)
        }
    }

    fun markInUse() = updateStatus(SpoolStatus.IN_USE)
    fun markNotInUse() = updateStatus(SpoolStatus.IN_STOCK)
    fun markUsedUp() = updateStatus(SpoolStatus.USED_UP)

    private fun updateStatus(newStatus: SpoolStatus) {
        viewModelScope.launch {
            val current = _spool.value ?: return@launch
            val updated = current.copy(
                status = newStatus,
                dateLastScanned = System.currentTimeMillis()
            )
            repository.update(updated)
            _spool.value = updated
        }
    }

    fun updateNotes(notes: String) {
        viewModelScope.launch {
            val current = _spool.value ?: return@launch
            val updated = current.copy(notes = notes)
            repository.update(updated)
            _spool.value = updated
        }
    }

    fun delete() {
        viewModelScope.launch {
            repository.delete(spoolId)
            _navigateBack.value = true
        }
    }
}
