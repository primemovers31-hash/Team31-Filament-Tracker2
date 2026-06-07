package com.bambu.nfc.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bambu.nfc.domain.model.Spool
import com.bambu.nfc.domain.repository.SpoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: SpoolRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val typeFilter = MutableStateFlow<String?>(null)
    val showUsedUp = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    val spools: StateFlow<List<Spool>> = combine(
        searchQuery, showUsedUp
    ) { query, usedUp ->
        query to usedUp
    }.flatMapLatest { (query, usedUp) ->
        when {
            query.isNotBlank() -> repository.search(query)
            usedUp -> repository.getUsedUp()
            else -> repository.getActive()
        }
    }.combine(typeFilter) { spools, type ->
        if (type != null) {
            spools.filter { it.filament.filamentType.equals(type, ignoreCase = true) }
        } else {
            spools
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearch(query: String) {
        searchQuery.value = query
    }

    fun setTypeFilter(type: String?) {
        typeFilter.value = type
    }

    fun setShowUsedUp(show: Boolean) {
        showUsedUp.value = show
    }
}
