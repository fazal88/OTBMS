package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.*
import kotlinx.datetime.*

data class DonationCollectionListScreen(private val initialFilters: DonationCollectionFilters = DonationCollectionFilters()) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DonationCollectionListViewModel>()
        val collections by viewModel.collections.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()
        val sortOrder by viewModel.sortOrder.collectAsState()
        val filters by viewModel.filters.collectAsState()
        val error by viewModel.error.collectAsState()
        val user by viewModel.currentUser.collectAsState()
        val selectedIds by viewModel.selectedIds.collectAsState()
        val isProcessing by viewModel.isProcessing.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(Unit) {
            if (initialFilters != DonationCollectionFilters()) {
                viewModel.updateFilters(initialFilters)
            }
        }

        DonationCollectionListContent(
            collections = collections,
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::updateSearchQuery,
            sortOrder = sortOrder,
            onSortOrderChange = viewModel::updateSortOrder,
            filters = filters,
            onFiltersChange = viewModel::updateFilters,
            onResetFilters = viewModel::resetFilters,
            onBack = { navigator.pop() },
            error = error,
            user = user,
            selectedIds = selectedIds,
            onToggleSelection = viewModel::toggleSelection,
            onConfirmSelected = viewModel::confirmSelectedReceived,
            onClearSelection = viewModel::clearSelection,
            isProcessing = isProcessing,
            onConfirmReceived = viewModel::confirmReceived,
            onBoxClick = { id -> navigator.push(DonationBoxDetailScreen(id)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationCollectionListContent(
    collections: List<DonationCollection>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOrder: CollectionSortOrder,
    onSortOrderChange: (CollectionSortOrder) -> Unit,
    filters: DonationCollectionFilters,
    onFiltersChange: (DonationCollectionFilters) -> Unit,
    onResetFilters: () -> Unit,
    onBack: () -> Unit,
    error: String?,
    user: User?,
    selectedIds: Set<String>,
    onToggleSelection: (String) -> Unit,
    onConfirmSelected: () -> Unit,
    onClearSelection: () -> Unit,
    isProcessing: Boolean,
    onConfirmReceived: (String) -> Unit,
    onBoxClick: (String) -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val isFilterApplied = filters != DonationCollectionFilters() || searchQuery.isNotEmpty()
    val canApprove = user?.role == UserRole.APPROVER || user?.role == UserRole.SUPER_ADMIN

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
                if (selectedIds.isNotEmpty()) {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = onClearSelection) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                            }
                        },
                        title = { Text("${selectedIds.size} selected") },
                        actions = {
                            if (canApprove) {
                                Button(
                                    onClick = onConfirmSelected,
                                    enabled = !isProcessing,
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    if (isProcessing) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White)
                                    else Text("Mark Received", fontSize = 12.sp)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                } else {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        title = {
                            if (isSearchActive) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    placeholder = { Text("Search collections...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { 
                                            onSearchQueryChange("")
                                            isSearchActive = false 
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close Search")
                                        }
                                    }
                                )
                            } else {
                                Text("Collections", fontWeight = FontWeight.ExtraBold)
                            }
                        },
                        actions = {
                            if (!isSearchActive) {
                                if (canApprove && collections.any { it.status == CollectionStatus.PENDING }) {
                                    TextButton(onClick = { 
                                        collections.filter { it.status == CollectionStatus.PENDING }.forEach {
                                            onToggleSelection(it.collectionId)
                                        }
                                    }) {
                                        Text("Select Pending")
                                    }
                                }
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
                
                if (isFilterApplied) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Filters:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        
                        if (searchQuery.isNotEmpty()) {
                            FilterChip(
                                selected = true,
                                onClick = { onSearchQueryChange("") },
                                label = { Text("Search: $searchQuery") },
                                trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) }
                            )
                        }

                        if (filters.status != null) {
                            FilterChip(
                                selected = true,
                                onClick = { onFiltersChange(filters.copy(status = null)) },
                                label = { Text("Status: ${filters.status}") },
                                trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) }
                            )
                        }
                        
                        TextButton(onClick = { 
                            onResetFilters()
                            onSearchQueryChange("")
                        }) {
                            Text("Clear All", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        BadgedBox(
                            badge = {
                                if (filters != DonationCollectionFilters()) {
                                    Badge { Text("!", modifier = Modifier.padding(2.dp)) }
                                }
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Filter")
                    }

                    Button(
                        onClick = { isSortMenuExpanded = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sort")
                        
                        DropdownMenu(
                            expanded = isSortMenuExpanded,
                            onDismissRequest = { isSortMenuExpanded = false }
                        ) {
                            CollectionSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(when(order) {
                                            CollectionSortOrder.DATE_DESC -> "Date (Newest)"
                                            CollectionSortOrder.DATE_ASC -> "Date (Oldest)"
                                            CollectionSortOrder.AMOUNT_DESC -> "Amount (High-Low)"
                                            CollectionSortOrder.AMOUNT_ASC -> "Amount (Low-High)"
                                        })
                                    },
                                    onClick = {
                                        onSortOrderChange(order)
                                        isSortMenuExpanded = false
                                    },
                                    leadingIcon = {
                                        if (sortOrder == order) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (collections.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No collections found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(collections) { collection ->
                    CollectionListItem(
                        collection = collection,
                        user = user,
                        isSelected = selectedIds.contains(collection.collectionId),
                        onToggleSelection = { onToggleSelection(collection.collectionId) },
                        onConfirmReceived = onConfirmReceived,
                        onBoxClick = { onBoxClick(collection.donationBoxId) }
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        CollectionFilterBottomSheet(
            filters = filters,
            onDismiss = { showFilterSheet = false },
            onApply = { 
                onFiltersChange(it)
                showFilterSheet = false
            }
        )
    }
}

@Composable
fun CollectionListItem(
    collection: DonationCollection,
    user: User?,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onConfirmReceived: (String) -> Unit,
    onBoxClick: () -> Unit
) {
    val canApprove = user?.role == UserRole.APPROVER || user?.role == UserRole.SUPER_ADMIN

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (canApprove) Modifier.clickable { onToggleSelection() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected && canApprove) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) 
                             else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canApprove) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelection() },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Column {
                        Text(
                            text = formatDate(collection.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "₹ ${collection.amountCollected}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                CollectionStatusBadge(collection.status)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = collection.collectorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onBoxClick() }.padding(vertical = 4.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Box ID: ${collection.donationBoxId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!collection.remarks.isNullOrBlank()) {
                Text(
                    text = "Remarks: ${collection.remarks}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (collection.status == CollectionStatus.PENDING) {
                if (canApprove) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { onConfirmReceived(collection.collectionId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Mark Received")
                    }
                }
            } else if (collection.status == CollectionStatus.RECEIVED) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Received by Treasury",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF386B1D),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionFilterBottomSheet(
    filters: DonationCollectionFilters,
    onDismiss: () -> Unit,
    onApply: (DonationCollectionFilters) -> Unit
) {
    var tempFilters by remember { mutableStateOf(filters) }
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filters", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { tempFilters = DonationCollectionFilters() }) {
                        Text("Reset")
                    }
                    Button(
                        onClick = { onApply(tempFilters) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Apply")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CollectionStatus.entries.forEach { status ->
                    FilterChip(
                        selected = tempFilters.status == status,
                        onClick = { tempFilters = tempFilters.copy(status = if (tempFilters.status == status) null else status) },
                        label = { Text(status.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.dayOfMonth} ${dateTime.month.name.take(3)}, ${dateTime.year} ${dateTime.hour}:${dateTime.minute}"
}
