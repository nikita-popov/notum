package xyz.polyserv.memos.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import xyz.polyserv.memos.R
import xyz.polyserv.memos.data.model.Memo
import xyz.polyserv.memos.presentation.ui.components.MemoCard
import xyz.polyserv.memos.presentation.ui.components.OfflineBanner
import xyz.polyserv.memos.presentation.viewmodel.MemoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoListScreen(
    viewModel: MemoViewModel = hiltViewModel(),
    onMemoClick: (Memo) -> Unit,
    onSettingsClick: () -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState = viewModel.uiState.value
    val memos by viewModel.memos.collectAsState()

    var showSearchBar by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!uiState.isOnline) {
            // TODO
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Offline Banner
        AnimatedVisibility(visible = !uiState.isOnline) {
            OfflineBanner(
                onSyncClick = { viewModel.syncPendingChanges() },
                isSyncing = uiState.syncInProgress
            )
        }

        // Error Dialog
        if (uiState.error != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                confirmButton = {
                    Button(onClick = { viewModel.clearError() }) {
                        Text(stringResource(id = R.string.ok))
                    }
                },
                title = { Text(stringResource(id = R.string.error)) },
                text = { Text(uiState.error) }
            )
        }

        // Top App Bar
        TopAppBar(
            title = {
                if (showSearchBar) {
                    SearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = { viewModel.searchMemos(it) },
                        onClose = {
                            showSearchBar = false
                            viewModel.clearSearch()
                        }
                    )
                } else {
                    Text(stringResource(id = R.string.my_notes))
                }
            },
            actions = {
                IconButton(
                    onClick = { showSearchBar = !showSearchBar }
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = { viewModel.syncNow() }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(id = R.string.sync))
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        )

        // Memos List or Search Results
        val displayMemos = if (uiState.searchQuery.isNotEmpty()) {
            uiState.filteredMemos
        } else {
            memos
        }

        if (displayMemos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(id = R.string.no_notes),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        stringResource(id = R.string.create_first_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp)
            ) {
                items(displayMemos) { memo ->
                    MemoCard(
                        memo = memo,
                        onClick = { onMemoClick(memo) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // FAB
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Memo")
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = { Text(stringResource(id = R.string.search)) },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            }
        }
    )
}
