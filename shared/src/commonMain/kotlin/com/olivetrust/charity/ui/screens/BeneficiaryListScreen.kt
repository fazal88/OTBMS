package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import com.olivetrust.charity.ui.previews.PreviewMocks

class BeneficiaryListScreen(private val initialFilters: BeneficiaryFilters = BeneficiaryFilters()) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<BeneficiaryListViewModel>()
        val beneficiaries by viewModel.beneficiaries.collectAsState()
        val totalCount by viewModel.totalCount.collectAsState()
        val searchQuery by viewModel.searchQuery.collectAsState()
        val sortOrder by viewModel.sortOrder.collectAsState()
        val filters by viewModel.filters.collectAsState()
        val error by viewModel.error.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(Unit) {
            if (initialFilters != BeneficiaryFilters()) {
                viewModel.updateFilters(initialFilters)
            }
        }

        BeneficiaryListContent(
            beneficiaries = beneficiaries,
            totalCount = totalCount,
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::updateSearchQuery,
            sortOrder = sortOrder,
            onSortOrderChange = viewModel::updateSortOrder,
            filters = filters,
            onFiltersChange = viewModel::updateFilters,
            onResetFilters = viewModel::resetFilters,
            onBack = { navigator.pop() },
            error = error,
            onDismissError = { viewModel.clearError() },
            onBeneficiaryClick = { id -> navigator.push(BeneficiaryDetailScreen(id)) },
            onAidClick = { id, name -> navigator.push(AidDistributionScreen(id, name)) },
            onVisitClick = { id, name -> navigator.push(VerificationVisitScreen(id, name)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiaryListContent(
    beneficiaries: List<Beneficiary>,
    totalCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    filters: BeneficiaryFilters,
    onFiltersChange: (BeneficiaryFilters) -> Unit,
    onResetFilters: () -> Unit,
    onBack: () -> Unit,
    error: String?,
    onDismissError: () -> Unit,
    onBeneficiaryClick: (String) -> Unit,
    onAidClick: (String, String) -> Unit,
    onVisitClick: (String, String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var isSearchActive by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val isFilterApplied = filters != BeneficiaryFilters() || searchQuery.isNotEmpty()

    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            onDismissError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                placeholder = { Text("Search...") },
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Beneficiaries", fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = if (isFilterApplied) "${beneficiaries.size} / $totalCount" else "$totalCount",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (!isSearchActive) {
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
                
                // Active Filters Row (Horizontal Scroll)
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
                                label = { Text(filters.status.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) },
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
                        if (filters.natureOfAid != null) {
                            FilterChip(
                                selected = true,
                                onClick = { onFiltersChange(filters.copy(natureOfAid = null)) },
                                label = { Text("Aid: ${filters.natureOfAid}") },
                                trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) }
                            )
                        }
                        if (filters.minPackets != null || filters.maxPackets != null) {
                            FilterChip(
                                selected = true,
                                onClick = { onFiltersChange(filters.copy(minPackets = null, maxPackets = null)) },
                                label = { Text("Packets: ${filters.minPackets ?: 0}-${filters.maxPackets ?: "∞"}") },
                                trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) }
                            )
                        }
                        if (filters.minAmount != null || filters.maxAmount != null) {
                            FilterChip(
                                selected = true,
                                onClick = { onFiltersChange(filters.copy(minAmount = null, maxAmount = null)) },
                                label = { Text("Amount: ${filters.minAmount ?: 0}-${filters.maxAmount ?: "∞"}") },
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
                                if (filters != BeneficiaryFilters()) {
                                    Badge { Text("!", modifier = Modifier.padding(2.dp)) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.List, null, modifier = Modifier.size(18.dp))
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
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sort")
                        
                        DropdownMenu(
                            expanded = isSortMenuExpanded,
                            onDismissRequest = { isSortMenuExpanded = false }
                        ) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(when(order) {
                                            SortOrder.NAME_ASC -> "Name (A-Z)"
                                            SortOrder.NAME_DESC -> "Name (Z-A)"
                                            SortOrder.DATE_ADDED_ASC -> "Date Added (Oldest)"
                                            SortOrder.DATE_ADDED_DESC -> "Date Added (Newest)"
                                            SortOrder.DATE_UPDATED_ASC -> "Date Updated (Oldest)"
                                            SortOrder.DATE_UPDATED_DESC -> "Date Updated (Newest)"
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
        if (beneficiaries.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(16.dp))
                    Text("No results matching your criteria", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                    if (isFilterApplied) {
                        Button(onClick = { 
                            onResetFilters()
                            onSearchQueryChange("")
                        }, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Reset All")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(beneficiaries) { beneficiary ->
                    BeneficiaryCard(
                        beneficiary = beneficiary,
                        onClick = { onBeneficiaryClick(beneficiary.id) },
                        onCallClick = {
                            if (beneficiary.phoneNumber.isNotEmpty()) {
                                uriHandler.openUri("tel:${beneficiary.phoneNumber}")
                            }
                        },
                        onAidClick = { onAidClick(beneficiary.id, beneficiary.headName) },
                        onVisitClick = { onVisitClick(beneficiary.id, beneficiary.headName) }
                    )
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
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
fun BeneficiaryCard(
    beneficiary: Beneficiary,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onAidClick: () -> Unit,
    onVisitClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with status and call
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ListStatusBadge(beneficiary.status)
                
                Surface(
                    onClick = onCallClick,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Call, "Call", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Main Info
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = beneficiary.headName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = beneficiary.areaCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${beneficiary.headAge} yrs • ${beneficiary.numberOfDependants} dependants",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            // Details Grid
            Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    InfoItem(Icons.Default.Add, "Nature", beneficiary.natureOfAid ?: "Pending")
                    InfoItem(Icons.AutoMirrored.Filled.List, "Packets", beneficiary.packetCount?.toString() ?: "—")
                }
                Column(modifier = Modifier.weight(1f)) {
                    InfoItem(Icons.Default.CheckCircle, "Monetary", beneficiary.monetaryAidAmount?.let { "₹ $it" } ?: "—")
                    InfoItem(Icons.Default.Info, "Reason", beneficiary.reasonForAid, maxLines = 1)
                }
            }

            if (!beneficiary.approvalNotes.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                        .padding(8.dp)
                ) {
                    Row {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = beneficiary.approvalNotes,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Footer Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onVisitClick,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Visit", fontSize = 12.sp)
                }
                
                Button(
                    onClick = onAidClick,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(0.0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Give Aid", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun InfoItem(icon: ImageVector, label: String, value: String, maxLines: Int = 1) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 10.sp)
            Text(
                value, 
                style = MaterialTheme.typography.bodySmall, 
                fontWeight = FontWeight.SemiBold, 
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ListStatusBadge(status: BeneficiaryStatus) {
    val (color, icon) = when (status) {
        BeneficiaryStatus.APPROVED -> Color(0xFF4CAF50) to Icons.Default.CheckCircle
        BeneficiaryStatus.PENDING_APPROVAL -> Color(0xFFFF9800) to Icons.Default.Refresh
        BeneficiaryStatus.REJECTED -> MaterialTheme.colorScheme.error to Icons.Default.Close
        BeneficiaryStatus.REAPPROVAL_PENDING -> Color(0xFF2196F3) to Icons.Default.Refresh
        BeneficiaryStatus.MISUSE_REPORTED -> MaterialTheme.colorScheme.error to Icons.Default.Warning
        BeneficiaryStatus.EDIT_REQUESTED -> Color(0xFF9C27B0) to Icons.Default.Edit
        else -> MaterialTheme.colorScheme.outline to Icons.Default.Info
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
                text = status.name.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
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
fun FilterBottomSheet(
    filters: BeneficiaryFilters,
    onDismiss: () -> Unit,
    onApply: (BeneficiaryFilters) -> Unit
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
                    TextButton(onClick = { tempFilters = BeneficiaryFilters() }) {
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

            // Status
            Text("Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BeneficiaryStatus.entries.forEach { status ->
                    FilterChip(
                        selected = tempFilters.status == status,
                        onClick = { tempFilters = tempFilters.copy(status = if (tempFilters.status == status) null else status) },
                        label = { Text(status.name.replace("_", " ").lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Area Code
            OutlinedTextField(
                value = tempFilters.areaCode ?: "",
                onValueChange = { tempFilters = tempFilters.copy(areaCode = it.ifBlank { null }) },
                label = { Text("Area Code") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Nature of Aid
            OutlinedTextField(
                value = tempFilters.natureOfAid ?: "",
                onValueChange = { tempFilters = tempFilters.copy(natureOfAid = it.ifBlank { null }) },
                label = { Text("Nature of Aid") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Packet Count Range
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tempFilters.minPackets?.toString() ?: "",
                    onValueChange = { tempFilters = tempFilters.copy(minPackets = it.toIntOrNull()) },
                    label = { Text("Min Packets") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = tempFilters.maxPackets?.toString() ?: "",
                    onValueChange = { tempFilters = tempFilters.copy(maxPackets = it.toIntOrNull()) },
                    label = { Text("Max Packets") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Amount Range
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tempFilters.minAmount?.toString() ?: "",
                    onValueChange = { tempFilters = tempFilters.copy(minAmount = it.toDoubleOrNull()) },
                    label = { Text("Min Amount") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = tempFilters.maxAmount?.toString() ?: "",
                    onValueChange = { tempFilters = tempFilters.copy(maxAmount = it.toDoubleOrNull()) },
                    label = { Text("Max Amount") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nature of Address
            OutlinedTextField(
                value = tempFilters.natureOfAddress ?: "",
                onValueChange = { tempFilters = tempFilters.copy(natureOfAddress = it.ifBlank { null }) },
                label = { Text("Nature of Address") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Reason for Aid
            OutlinedTextField(
                value = tempFilters.reasonForAid ?: "",
                onValueChange = { tempFilters = tempFilters.copy(reasonForAid = it.ifBlank { null }) },
                label = { Text("Reason for Aid") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Preview
@Composable
fun BeneficiaryListContentPreview() {
    MaterialTheme {
        BeneficiaryListContent(
            beneficiaries = listOf(
                PreviewMocks.mockBeneficiary.copy(
                    natureOfAid = "Ration",
                    packetCount = 2,
                    monthlyRation = "10kg Flour, 2kg Sugar",
                    reasonForAid = "Low income labor",
                    headAge = 45,
                    numberOfDependants = 5,
                    monetaryAidAmount = 5000.0,
                    approvalNotes = "Verified deserving case for monthly ration and monetary support."
                ),
                PreviewMocks.mockBeneficiary.copy(
                    id = "b2",
                    headName = "John Doe",
                    phoneNumber = "9876543210",
                    areaCode = "LHR-02"
                )
            ),
            totalCount = 2,
            searchQuery = "",
            onSearchQueryChange = {},
            sortOrder = SortOrder.DATE_ADDED_DESC,
            onSortOrderChange = {},
            filters = BeneficiaryFilters(),
            onFiltersChange = {},
            onResetFilters = {},
            onBack = {},
            error = null,
            onDismissError = {},
            onBeneficiaryClick = {},
            onAidClick = { _, _ -> },
            onVisitClick = { _, _ -> }
        )
    }
}
