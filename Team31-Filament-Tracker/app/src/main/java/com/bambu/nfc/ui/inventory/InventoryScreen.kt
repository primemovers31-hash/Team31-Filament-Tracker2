package com.bambu.nfc.ui.inventory

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onSpoolClick: (Long) -> Unit
) {
    val spools by viewModel.spools.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val showUsedUp by viewModel.showUsedUp.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Tabs: Active inventory vs Used Up (reorder list)
        TabRow(selectedTabIndex = if (showUsedUp) 1 else 0) {
            Tab(
                selected = !showUsedUp,
                onClick = { viewModel.setShowUsedUp(false) },
                text = { Text("Inventory") }
            )
            Tab(
                selected = showUsedUp,
                onClick = { viewModel.setShowUsedUp(true) },
                text = { Text("Used Up") }
            )
        }

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearch(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search filament...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.setSearch("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        // Type filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val types = listOf("PLA", "PETG", "ABS", "TPU", "ASA", "PA")
            types.forEach { type ->
                FilterChip(
                    selected = typeFilter == type,
                    onClick = {
                        viewModel.setTypeFilter(if (typeFilter == type) null else type)
                    },
                    label = { Text(type) }
                )
            }
        }

        // List
        if (spools.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = when {
                        searchQuery.isNotBlank() || typeFilter != null -> "No spools match your filters"
                        showUsedUp -> "No used up spools yet. When you finish a spool, it will appear here for reordering."
                        else -> "No spools yet. Scan a filament spool to get started!"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(spools, key = { it.id }) { spool ->
                    SpoolCard(
                        spool = spool,
                        onClick = { onSpoolClick(spool.id) }
                    )
                }
            }
        }
    }
}
