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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.*
import com.olivetrust.charity.openMaps
import kotlinx.datetime.*
import org.koin.core.parameter.parametersOf

class DonationBoxDetailScreen(private val boxId: String) : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DonationBoxDetailViewModel> { parametersOf(boxId) }
        val box by viewModel.box.collectAsState()
        val collections by viewModel.collections.collectAsState()
        val issues by viewModel.issues.collectAsState()
        val auditLogs by viewModel.auditLogs.collectAsState()
        val user by viewModel.currentUser.collectAsState()
        val isProcessing by viewModel.isProcessing.collectAsState()
        val error by viewModel.error.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        DonationBoxDetailContent(
            box = box,
            collections = collections,
            issues = issues,
            auditLogs = auditLogs,
            user = user,
            isProcessing = isProcessing,
            error = error,
            onBack = { navigator.pop() },
            onRecordCollection = { navigator.push(RecordCollectionScreen(boxId)) },
            onReportIssue = { navigator.push(ReportIssueScreen(boxId)) },
            onApprove = viewModel::approveBox,
            onReject = viewModel::rejectBox,
            onApproveIssue = viewModel::approveIssue,
            onRejectIssue = viewModel::rejectIssue,
            onClearError = viewModel::clearError
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationBoxDetailContent(
    box: DonationBox?,
    collections: List<DonationCollection>,
    issues: List<DonationBoxIssue>,
    auditLogs: List<AuditLog>,
    user: User?,
    isProcessing: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRecordCollection: () -> Unit,
    onReportIssue: () -> Unit,
    onApprove: () -> Unit,
    onReject: (String) -> Unit,
    onApproveIssue: (String, DonationBoxStatus, String) -> Unit,
    onRejectIssue: (String, String) -> Unit,
    onClearError: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showIssueReviewDialog by remember { mutableStateOf<DonationBoxIssue?>(null) }
    
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Box Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            if (box != null && user != null) {
                Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (user.role == UserRole.COLLECTOR && box.status == DonationBoxStatus.APPROVED_ACTIVE) {
                            Button(
                                onClick = onReportIssue,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Report Issue")
                            }
                            Button(
                                onClick = onRecordCollection,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Record Collection")
                            }
                        } else if (user.role == UserRole.APPROVER && box.status == DonationBoxStatus.PENDING_APPROVAL) {
                            OutlinedButton(
                                onClick = { showRejectDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reject")
                            }
                            Button(
                                onClick = onApprove,
                                modifier = Modifier.weight(1f),
                                enabled = !isProcessing
                            ) {
                                if (isProcessing) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                                else Text("Approve")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (box == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            var selectedTab by remember { mutableIntStateOf(0) }
            Column(modifier = Modifier.padding(padding)) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Info", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("Collections", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                        Text("Issues", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }) {
                        Text("Logs", modifier = Modifier.padding(12.dp))
                    }
                }

                when (selectedTab) {
                    0 -> BoxInfoTab(box, uriHandler)
                    1 -> BoxCollectionsTab(collections, box, user)
                    2 -> BoxIssuesTab(issues, user, onReviewIssue = { showIssueReviewDialog = it })
                    3 -> BoxLogsTab(auditLogs)
                }
            }
        }
    }

    if (showRejectDialog) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Donation Box") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Rejection Reason") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onReject(reason)
                    showRejectDialog = false
                }) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showIssueReviewDialog != null) {
        val issue = showIssueReviewDialog!!
        var notes by remember { mutableStateOf("") }
        var selectedStatus by remember { mutableStateOf(DonationBoxStatus.OUT_OF_ORDER) }
        
        AlertDialog(
            onDismissRequest = { showIssueReviewDialog = null },
            title = { Text("Review Issue: ${issue.reportType}") },
            text = {
                Column {
                    Text("Collector's Description: ${issue.description}")
                    Spacer(Modifier.height(16.dp))
                    Text("Set Box Status To:")
                    Row(Modifier.horizontalScroll(rememberScrollState())) {
                        listOf(DonationBoxStatus.APPROVED_ACTIVE, DonationBoxStatus.OUT_OF_ORDER, DonationBoxStatus.DECOMMISSIONED).forEach { status ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedStatus = status }) {
                                RadioButton(selected = selectedStatus == status, onClick = { selectedStatus = status })
                                Text(status.name.lowercase().replaceFirstChar { it.titlecase() })
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Review Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        onRejectIssue(issue.issueId, notes)
                        showIssueReviewDialog = null
                    }) {
                        Text("Reject Report", color = MaterialTheme.colorScheme.error)
                    }
                    Button(onClick = {
                        onApproveIssue(issue.issueId, selectedStatus, notes)
                        showIssueReviewDialog = null
                    }) {
                        Text("Approve & Update Status")
                    }
                }
            }
        )
    }
}

@Composable
fun BoxInfoTab(box: DonationBox, uriHandler: UriHandler) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Basic Information", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                DetailItem("ID", box.id)
                DetailItem("Address", box.address)
                DetailItem("Area Code", box.areaCode)
                DetailItem("Installation Date", formatDate(box.installationDate))
                DetailItem("Installed By", box.installedBy)
                DetailItem("Status", box.status.name.replace("_", " "))
            }
        }

        Card(shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Contact Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                DetailItem("Person of Contact", box.personOfContact)
                DetailItem("Contact Number", box.contactNumber)
                
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { uriHandler.openUri("tel:${box.contactNumber}") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Call POC")
                    }
                    Button(
                        onClick = { openMaps(box.latitude, box.longitude, box.address) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.LocationOn, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Get Directions")
                    }
                }
            }
        }

        if (box.rejectionReason != null) {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Rejection Reason", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    Text(box.rejectionReason, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

@Composable
fun BoxCollectionsTab(collections: List<DonationCollection>, box: DonationBox, user: User?) {
    val uriHandler = LocalUriHandler.current
    if (collections.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No collections recorded yet")
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(collections) { collection ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatDate(collection.timestamp), fontWeight = FontWeight.Bold)
                            Text("₹ ${collection.amountCollected}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                        }
                        Text("Collected by: ${collection.collectorName}", style = MaterialTheme.typography.bodySmall)
                        if (!collection.remarks.isNullOrBlank()) {
                            Text("Remarks: ${collection.remarks}", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { 
                                val message = """
                                    Dear ${box.personOfContact},
                                    Greetings from Olive Trust.
                                    We have successfully collected the donation from your Donation Box.
                                    Collection Details
                                    • Date: ${formatDate(collection.timestamp)}
                                    • Amount Collected: ₹ ${collection.amountCollected}
                                    • Collected By: ${collection.collectorName}
                                    Thank you for your generous support.
                                """.trimIndent()
                                uriHandler.openUri("whatsapp://send?phone=${box.contactNumber}&text=${message.encodeURL()}")
                            }) {
                                Icon(Icons.Default.Share, "Share Receipt")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoxIssuesTab(issues: List<DonationBoxIssue>, user: User?, onReviewIssue: (DonationBoxIssue) -> Unit) {
    if (issues.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No issues reported")
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(issues) { issue ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(issue.reportType.name.replace("_", " "), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text(issue.status.name, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(formatDate(issue.timestamp), style = MaterialTheme.typography.bodySmall)
                        Text(issue.description)
                        
                        if (user?.role == UserRole.APPROVER && issue.status == IssueStatus.PENDING_REVIEW) {
                            Button(onClick = { onReviewIssue(issue) }, modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) {
                                Text("Review")
                            }
                        }
                        
                        if (issue.status != IssueStatus.PENDING_REVIEW) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Text("Review Notes: ${issue.reviewNotes ?: "None"}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoxLogsTab(logs: List<AuditLog>) {
    if (logs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No logs available")
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(logs) { log ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(log.actionType, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("By ${log.userId} (${log.role})", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(formatDate(log.timestamp), style = MaterialTheme.typography.labelSmall)
                }
                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.width(120.dp), color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun formatDate(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.dayOfMonth} ${dateTime.month.name.take(3)}, ${dateTime.year} ${dateTime.hour}:${dateTime.minute}"
}

private fun String.encodeURL(): String = this.replace(" ", "%20").replace("\n", "%0A")
