package xyz.polyserv.notum.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import timber.log.Timber
import xyz.polyserv.notum.data.model.Memo
import xyz.polyserv.notum.presentation.ui.components.MarkdownText
import xyz.polyserv.notum.presentation.ui.components.SyncStatusIndicator
import xyz.polyserv.notum.presentation.viewmodel.MemoViewModel
import xyz.polyserv.notum.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoDetailScreen(
    memoId: String,
    modifier: Modifier = Modifier,
    viewModel: MemoViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onEditClick: (Memo) -> Unit
) {
    val uiState = viewModel.uiState.value
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(memoId) {
        viewModel.loadMemoById(memoId)
    }

    val memo = uiState.selectedMemo

    if (showDeleteDialog && memo != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(id = R.string.delete_note)) },
            text = { Text(stringResource(id = R.string.this_action_cant_be_undone)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMemo(memo.id)
                        showDeleteDialog = false
                        onBackClick()
                    }
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.note)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    if (memo != null) {
                        IconButton(onClick = { onEditClick(memo) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.edit))
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (memo != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Timber.d("Opened memo: ID=${memo.id}, name=${memo.name}")
                    Timber.d("Opened memo: ${memo.content}")

                    SyncStatusIndicator(syncStatus = memo.syncStatus)

                    /*Text(
                        text = memo.content.ifEmpty { stringResource(id = R.string.no_content) },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 16.dp)
                    )*/
                    MarkdownText(
                        markdown = memo.content.ifEmpty { "No content" },
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Metadata
                    val createTimeText = memo.getFormattedCreateTime(context)
                    if (createTimeText.isNotEmpty()) {
                        Text(
                            text = "${stringResource(id = R.string.created)}: $createTimeText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (memo.getUpdateTimestamp() > memo.getCreateTimestamp()) {
                        val updateTimeText = memo.getFormattedUpdateTime(context)
                        if (updateTimeText.isNotEmpty()) {
                            Text(
                                text = "${stringResource(id = R.string.updated)}: $updateTimeText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(stringResource(id = R.string.memo_not_found))
                }
            }
        }
    }
}
