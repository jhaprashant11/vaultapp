package com.localvault.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.localvault.app.R
import com.localvault.app.data.PasswordCategory
import com.localvault.app.data.PasswordEntry
import com.localvault.app.data.PasswordRepository
import com.localvault.app.ui.components.categoryIcon
import com.localvault.app.util.copyToClipboardWithTimeout
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    entryId: String,
    repository: PasswordRepository,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var entry by remember { mutableStateOf<PasswordEntry?>(null) }
    var reveal by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(entryId) {
        entry = repository.getById(entryId)
        loaded = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(entry?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = onEdit, enabled = entry != null) {
                        Text(stringResource(R.string.edit))
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            !loaded -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            entry == null -> {
                Text(
                    "Not found",
                    modifier = Modifier.padding(padding).padding(24.dp),
                )
            }
            else -> {
                val e = entry!!
                val category = PasswordCategory.fromId(e.category)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = CircleShape,
                            ) {
                                Icon(
                                    imageVector = categoryIcon(category),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(12.dp),
                                )
                            }
                            Column {
                                Text(e.title.ifBlank { "(no title)" }, style = MaterialTheme.typography.titleLarge)
                                Text(category.label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f))
                            }
                        }
                    }
                    Text(stringResource(R.string.username_label), style = MaterialTheme.typography.labelMedium)
                    Text(e.username, style = MaterialTheme.typography.bodyLarge)
                    OutlinedButton(onClick = {
                        copyToClipboardWithTimeout(context, "username", e.username)
                    }) {
                        Text(stringResource(R.string.copy_username))
                    }
                    Text(stringResource(R.string.password_label), style = MaterialTheme.typography.labelMedium)
                    Text(
                        if (reveal) e.password else "********",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = { reveal = !reveal }) {
                        Text(
                            if (reveal) stringResource(R.string.hide_password)
                            else stringResource(R.string.reveal_password),
                        )
                    }
                    OutlinedButton(onClick = {
                        copyToClipboardWithTimeout(context, "password", e.password)
                    }) {
                        Text(stringResource(R.string.copy_password))
                    }
                    e.url?.takeIf { it.isNotBlank() }?.let { url ->
                        Text(stringResource(R.string.url_label), style = MaterialTheme.typography.labelMedium)
                        Text(url, style = MaterialTheme.typography.bodyMedium)
                    }
                    e.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                        Text(stringResource(R.string.notes_label), style = MaterialTheme.typography.labelMedium)
                        Text(notes, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                repository.delete(e)
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                }
            }
        }
    }
}
