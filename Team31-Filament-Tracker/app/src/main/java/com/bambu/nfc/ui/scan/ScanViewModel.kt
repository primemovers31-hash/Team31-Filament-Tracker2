package com.bambu.nfc.ui.scan

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bambu.nfc.data.nfc.BambuTagParser
import com.bambu.nfc.data.nfc.BambuTagReader
import com.bambu.nfc.domain.model.FilamentData
import com.bambu.nfc.domain.model.Spool
import com.bambu.nfc.domain.usecase.ProcessScannedTagUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val tagReader: BambuTagReader,
    private val tagParser: BambuTagParser,
    private val processTag: ProcessScannedTagUseCase
) : ViewModel() {

    sealed class UiState {
        data object Ready : UiState()
        data object Scanning : UiState()
        data class ChooseAction(
            val filament: FilamentData,
            val existingSpool: Spool?
        ) : UiState()
        data class Done(val spool: Spool, val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Ready)
    val uiState: StateFlow<UiState> = _uiState

    fun onTagDiscovered(tag: Tag) {
        viewModelScope.launch {
            _uiState.value = UiState.Scanning

            when (val readResult = tagReader.readTag(tag)) {
                is BambuTagReader.ReadResult.Error -> {
                    _uiState.value = UiState.Error(readResult.message)
                }
                is BambuTagReader.ReadResult.Success -> {
                    try {
                        val filament = tagParser.parse(readResult.uid, readResult.blocks)
                        val scanInfo = processTag.lookup(filament)
                        _uiState.value = UiState.ChooseAction(
                            filament = scanInfo.filament,
                            existingSpool = scanInfo.existingSpool
                        )
                    } catch (e: Exception) {
                        _uiState.value = UiState.Error("Failed to parse tag data: ${e.message}")
                    }
                }
            }
        }
    }

    fun addToInventory(filament: FilamentData) {
        viewModelScope.launch {
            val spool = processTag.addToInventory(filament)
            _uiState.value = UiState.Done(spool, "Added to inventory")
        }
    }

    fun markInUse(spool: Spool) {
        viewModelScope.launch {
            val updated = processTag.markInUse(spool)
            _uiState.value = UiState.Done(updated, "Marked as in use")
        }
    }

    fun markNotInUse(spool: Spool) {
        viewModelScope.launch {
            val updated = processTag.markNotInUse(spool)
            _uiState.value = UiState.Done(updated, "Returned to stock")
        }
    }

    fun removeFromInventory(spool: Spool) {
        viewModelScope.launch {
            val updated = processTag.removeFromInventory(spool)
            _uiState.value = UiState.Done(updated, "Removed from inventory")
        }
    }

    fun dismiss() {
        _uiState.value = UiState.Ready
    }
}
