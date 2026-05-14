package com.localvault.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.localvault.app.R
import com.localvault.app.data.PasswordCategory
import com.localvault.app.data.PasswordEntry
import com.localvault.app.data.PasswordRepository
import com.localvault.app.ui.components.categoryIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: PasswordRepository,
    onOpenEntry: (String) -> Unit,
    onAddEntry: () -> Unit,
    onSettings: () -> Unit,
    onLock: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PasswordCategory?>(null) }
    var sortMode by remember { mutableStateOf(SortMode.Recent) }
    val entries by repository.search(query).collectAsState(initial = emptyList())
    val filteredEntries = selectedCategory?.let { category ->
        entries.filter { it.category == category.id }
    } ?: entries
    val visibleEntries = when (sortMode) {
        SortMode.Recent -> filteredEntries.sortedByDescending { it.updatedAt }
        SortMode.Name -> filteredEntries.sortedBy { it.title.lowercase() }
        SortMode.Category -> filteredEntries.sortedWith(
            compareBy<PasswordEntry> { PasswordCategory.fromId(it.category).label }
                .thenBy { it.title.lowercase() },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.vault_home)) },
                navigationIcon = {
                    IconButton(onClick = onLock) {
                        Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.lock))
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntry) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_entry))
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        ) {
            item {
                VaultSummary(entries.size)
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                CategoryFilters(
                    selectedCategory = selectedCategory,
                    onSelect = { selectedCategory = it },
                )
            }
            item {
                SortFilters(
                    sortMode = sortMode,
                    onSort = { sortMode = it },
                )
            }

            val categories = PasswordCategory.values().toList()
            categories.forEach { category ->
                val sectionEntries = visibleEntries.filter { it.category == category.id }
                if (sectionEntries.isNotEmpty()) {
                    item(key = "header-${category.id}") {
                        SectionHeader(category = category, count = sectionEntries.size)
                    }
                    items(sectionEntries, key = { it.id }) { item ->
                        EntryRow(item = item, onClick = { onOpenEntry(item.id) })
                    }
                }
            }
            if (visibleEntries.isEmpty()) {
                item {
                    EmptyVault(
                        selectedCategory = selectedCategory,
                        onAddEntry = onAddEntry,
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultSummary(count: Int) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.saved_items),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                )
                Text(
                    "$count",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(18.dp),
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CategoryFilters(
    selectedCategory: PasswordCategory?,
    onSelect: (PasswordCategory?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onSelect(null) },
            label = { Text("All") },
        )
    }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 8.dp),
    ) {
        items(PasswordCategory.values().toList()) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onSelect(if (selectedCategory == category) null else category) },
                label = { Text(category.label) },
                leadingIcon = {
                    Icon(
                        imageVector = categoryIcon(category),
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SortFilters(
    sortMode: SortMode,
    onSort: (SortMode) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 8.dp),
    ) {
        items(SortMode.values().toList()) { mode ->
            FilterChip(
                selected = sortMode == mode,
                onClick = { onSort(mode) },
                label = {
                    Text(
                        when (mode) {
                            SortMode.Recent -> stringResource(R.string.sort_recent)
                            SortMode.Name -> stringResource(R.string.sort_name)
                            SortMode.Category -> stringResource(R.string.sort_category)
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(category: PasswordCategory, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                sectionTitle(category),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            "$count",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
        )
    }
}

@Composable
private fun EntryRow(item: PasswordEntry, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = categoryIcon(PasswordCategory.fromId(item.category)),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    item.title.ifBlank { "(no title)" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    item.username.ifBlank { "No username" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                )
            }
            Text(
                "****",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
            )
        }
    }
}

@Composable
private fun EmptyVault(
    selectedCategory: PasswordCategory?,
    onAddEntry: () -> Unit,
) {
    val category = selectedCategory ?: PasswordCategory.General
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = CircleShape,
        ) {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(18.dp),
            )
        }
        Text(stringResource(R.string.no_entries), style = MaterialTheme.typography.titleMedium)
        Text(
            if (selectedCategory == null) stringResource(R.string.add_first_password)
            else "${stringResource(R.string.add_entry)}: ${category.label}",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable(onClick = onAddEntry),
        )
    }
}

@Composable
private fun sectionTitle(category: PasswordCategory): String =
    when (category) {
        PasswordCategory.Bank -> stringResource(R.string.bank_passwords)
        PasswordCategory.Upi -> stringResource(R.string.upi_passwords)
        PasswordCategory.Email -> stringResource(R.string.email_passwords)
        PasswordCategory.Game -> stringResource(R.string.game_passwords)
        PasswordCategory.General -> stringResource(R.string.other_passwords)
    }

private enum class SortMode {
    Recent,
    Name,
    Category,
}
