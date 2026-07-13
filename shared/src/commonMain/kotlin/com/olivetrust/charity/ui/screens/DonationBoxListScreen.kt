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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.*
import com.olivetrust.charity.domain.util.LocationUtil
import com.olivetrust.charity.openMaps
import com.olivetrust.charity.Location
import kotlinx.datetime.*

data class DonationBoxListScreen(private val initialFilters: DonationBoxFilters = DonationBoxFilters()) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DonationBoxListViewModel>()
        val boxes by viewModel.boxes.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()
        val sortOrder by viewModel.sortOrder.collectAsState()
        val filters by viewModel.filters.collectAsState()
        val error by viewModel.error.collectAsState()
        val user by viewModel.currentUser.collectAsState()
        val currentLocation by viewModel.currentLocation.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(Unit) {
            if (initialFilters != DonationBoxFilters()) {
                viewModel.updateFilters(initialFilters)
            }
        }

        DonationBoxListContent(
            boxes = boxes,
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
            currentLocation = currentLocation,
            currentTime = viewModel.currentTime,
            onBoxClick = { id -> navigator.push(DonationBoxDetailScreen(id)) },
            onInstallClick = { navigator.push(InstallDonationBoxScreen()) },
            onMapClick = { navigator.push(DonationBoxMapScreen(filters)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationBoxListContent(
    boxes: List<DonationBox>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOrder: BoxSortOrder,
    onSortOrderChange: (BoxSortOrder) -> Unit,
    filters: DonationBoxFilters,
    onFiltersChange: (DonationBoxFilters) -> Unit,
    onResetFilters: () -> Unit,
    onBack: () -> Unit,
    error: String?,
    user: User?,
    currentLocation: Location?,
    currentTime: Long,
    onBoxClick: (String) -> Unit,
    onInstallClick: () -> Unit,
    onMapClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var isSearchActive by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val isFilterApplied = filters != DonationBoxFilters() || searchQuery.isNotEmpty()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
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
                                placeholder = { Text("Search boxes...") },
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
                            Text("Donation Boxes", fontWeight = FontWeight.ExtraBold)
                            if (boxes.isNotEmpty()) {
                                Text(
                                    text = "${boxes.size} boxes found",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    actions = {
                        if (!isSearchActive) {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                            IconButton(onClick = onMapClick) {
                                Icon(Icons.Default.LocationOn, contentDescription = "Map View")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                
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
                                label = { 
                                    Text(when (filters.status) {
                                        DonationBoxStatus.ACTIVE -> "Active"
                                        DonationBoxStatus.PENDING_APPROVAL -> "Pending"
                                        else -> filters.status.name.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                    })
                                },
                                trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) }
                            )
                        }
                        if (filters.areaCode != null) {
                            FilterChip(
                                selected = true,
                                onClick = { onFiltersChange(filters.copy(areaCode = null)) },
                                label = { Text("Area: ${filters.areaCode}") },
                                trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) }
                            )
                        }
                        if (filters.overdueOnly) {
                            FilterChip(
                                selected = true,
                                onClick = { onFiltersChange(filters.copy(overdueOnly = false)) },
                                label = { Text("Overdue (>60 days)") },
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
        floatingActionButton = {
            if (user?.role == UserRole.COLLECTOR) {
                FloatingActionButton(
                    onClick = onInstallClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Install New Box")
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
                                if (filters != DonationBoxFilters()) {
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
                            BoxSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(when(order) {
                                            BoxSortOrder.ID_ASC -> "ID (A-Z)"
                                            BoxSortOrder.ID_DESC -> "ID (Z-A)"
                                            BoxSortOrder.INSTALLATION_DATE_ASC -> "Installed (Oldest)"
                                            BoxSortOrder.INSTALLATION_DATE_DESC -> "Installed (Newest)"
                                            BoxSortOrder.LAST_COLLECTION_DATE_ASC -> "Last Collection (Oldest)"
                                            BoxSortOrder.LAST_COLLECTION_DATE_DESC -> "Last Collection (Newest)"
                                            BoxSortOrder.LAST_UPDATED_ASC -> "Last Updated (Oldest)"
                                            BoxSortOrder.LAST_UPDATED_DESC -> "Last Updated (Newest)"
                                            BoxSortOrder.DISTANCE_ASC -> "Distance (Nearest)"
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
        if (boxes.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Text("No donation boxes found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                }
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
                items(boxes) { box ->
                    DonationBoxCard(
                        box = box,
                        currentLocation = currentLocation,
                        currentTime = currentTime,
                        onClick = { onBoxClick(box.id) },
                        onCallClick = {
                            if (box.contactNumber.isNotEmpty()) {
                                uriHandler.openUri("tel:${box.contactNumber}")
                            }
                        },
                        onDirectionsClick = {
                            openMaps(box.latitude, box.longitude, box.address)
                        }
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        DonationBoxFilterBottomSheet(
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
fun DonationBoxCard(
    box: DonationBox,
    currentLocation: Location?,
    currentTime: Long,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onDirectionsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DonationBoxStatusBadge(box.status)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentLocation != null) {
                        val distance = LocationUtil.calculateDistance(
                            currentLocation.latitude, currentLocation.longitude,
                            box.latitude, box.longitude
                        )
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = LocationUtil.formatDistance(distance),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    
                    val daysSince = remember(box.lastCollectionDate, box.installationDate, currentTime) {
                        val last = box.lastCollectionDate ?: box.installationDate
                        ((currentTime - last) / (24L * 60 * 60 * 1000)).toInt()
                    }
                    
                    Surface(
                        color = if (daysSince > 60) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "$daysSince days due",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (daysSince > 60) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = box.address,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${box.personOfContact} • ${box.areaCode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                BoxInfoItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DateRange,
                    label = "Last Collection",
                    value = box.lastCollectionDate?.let { formatDate(it) } ?: "Never"
                )
                BoxInfoItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ShoppingCart,
                    label = "Last Amount",
                    value = box.lastCollectedAmount?.let { "₹ $it" } ?: "—"
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCallClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Call", fontSize = 12.sp, maxLines = 1)
                }
                
                Button(
                    onClick = onDirectionsClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Directions", fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun BoxInfoItem(modifier: Modifier = Modifier, icon: ImageVector, label: String, value: String) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 10.sp)
            Text(
                value, 
                style = MaterialTheme.typography.bodySmall, 
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun DonationBoxStatusBadge(status: DonationBoxStatus) {
    val (color, icon) = when (status) {
        DonationBoxStatus.ACTIVE -> Color(0xFF4CAF50) to Icons.Default.CheckCircle
        DonationBoxStatus.PENDING_APPROVAL -> Color(0xFFFF9800) to Icons.Default.Refresh
        DonationBoxStatus.INACTIVE -> MaterialTheme.colorScheme.error to Icons.Default.Close
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(12.dp), tint = color)
            Text(
                text = when (status) {
                    DonationBoxStatus.ACTIVE -> "Active"
                    DonationBoxStatus.PENDING_APPROVAL -> "Pending"
                    else -> status.name.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                },
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationBoxFilterBottomSheet(
    filters: DonationBoxFilters,
    onDismiss: () -> Unit,
    onApply: (DonationBoxFilters) -> Unit
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
                    TextButton(onClick = { tempFilters = DonationBoxFilters() }) {
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
                DonationBoxStatus.entries.forEach { status ->
                    FilterChip(
                        selected = tempFilters.status == status,
                        onClick = { tempFilters = tempFilters.copy(status = if (tempFilters.status == status) null else status) },
                        label = { 
                            Text(when (status) {
                                DonationBoxStatus.ACTIVE -> "Active"
                                DonationBoxStatus.PENDING_APPROVAL -> "Pending"
                                else -> status.name.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            })
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = tempFilters.areaCode ?: "",
                onValueChange = { tempFilters = tempFilters.copy(areaCode = it.ifBlank { null }) },
                label = { Text("Area Code") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = tempFilters.overdueOnly,
                    onCheckedChange = { tempFilters = tempFilters.copy(overdueOnly = it) }
                )
                Text("Show Overdue Only (>60 days since last collection)")
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.dayOfMonth} ${dateTime.month.name.take(3)}, ${dateTime.year}"
}
