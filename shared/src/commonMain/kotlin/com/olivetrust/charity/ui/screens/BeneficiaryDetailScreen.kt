package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.olivetrust.charity.domain.model.FamilyMember
import com.olivetrust.charity.ui.previews.PreviewMocks
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.Beneficiary
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import com.olivetrust.charity.domain.model.User
import com.olivetrust.charity.domain.model.UserRole
import com.olivetrust.charity.domain.repository.AuthRepository
import com.olivetrust.charity.domain.repository.BeneficiaryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.ui.graphics.Color
import coil3.compose.AsyncImage
import com.olivetrust.charity.domain.model.*
import com.olivetrust.charity.domain.repository.*
import com.olivetrust.charity.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import androidx.compose.foundation.lazy.items
import com.olivetrust.charity.domain.util.LocationUtil
import com.olivetrust.charity.openMaps
import com.olivetrust.charity.openUrl
import com.olivetrust.charity.shareUrl
import com.olivetrust.charity.getFilePicker
import kotlin.time.Clock

class BeneficiaryDetailScreen(private val beneficiaryId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<BeneficiaryDetailViewModel>()
        val beneficiary by viewModel.beneficiary.collectAsState()
        val user by viewModel.currentUser.collectAsState()
        val visits by viewModel.visits.collectAsState()
        val distributions by viewModel.aidDistributions.collectAsState()
        val isUploading by viewModel.isUploading.collectAsState()
        val scope = rememberCoroutineScope()

        var showDeleteDialog by remember { mutableStateOf(false) }
        var showRejectDialog by remember { mutableStateOf(false) }
        var rejectionReason by remember { mutableStateOf("") }
        
        var showAddAttachmentDialog by remember { mutableStateOf(false) }
        var attachmentName by remember { mutableStateOf("") }
        var attachmentUrl by remember { mutableStateOf("") }
        
        var showPhotoDialog by remember { mutableStateOf(false) }

        val snackbarHostState = remember { SnackbarHostState() }

        var isRecording by remember { mutableStateOf(false) }
        val recorder = remember { getAudioRecorder() }
        var recordingStartTime by remember { mutableStateOf(0L) }

        LaunchedEffect(beneficiaryId) {
            viewModel.loadBeneficiary(beneficiaryId)
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Beneficiary Details", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    })
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (isUploading) return@FloatingActionButton
                        if (isRecording) {
                            isRecording = false
                            val data = recorder.stopRecording()
                            val duration = Clock.System.now().toEpochMilliseconds() - recordingStartTime
                            if (data != null) {
                                beneficiary?.let { b ->
                                    scope.launch {
                                        viewModel.uploadAndAddDiscussionNote(b.id, data, duration)
                                        snackbarHostState.showSnackbar("Voice note uploaded")
                                    }
                                }
                            }
                        } else {
                            isRecording = true
                            recordingStartTime = Clock.System.now().toEpochMilliseconds()
                            recorder.startRecording()
                        }
                    },
                    containerColor = when {
                        isUploading -> MaterialTheme.colorScheme.surfaceVariant
                        isRecording -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                    contentColor = when {
                        isUploading -> MaterialTheme.colorScheme.onSurfaceVariant
                        isRecording -> MaterialTheme.colorScheme.onError
                        else -> MaterialTheme.colorScheme.onPrimary
                    }
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Close else Icons.Default.Call,
                            contentDescription = if (isRecording) "Stop Recording" else "Record Voice Note"
                        )
                    }
                }
            }
        ) { padding ->
            beneficiary?.let { b ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            HeaderSection(
                                b, 
                                onPhotoClick = { showPhotoDialog = true }
                            )
                        }

                        item {
                            ActionButtons(
                                b,
                                user,
                                onEdit = { navigator.push(EditBeneficiaryScreen(b)) },
                                onDelete = { showDeleteDialog = true },
                                onApprove = { navigator.push(ApproveBeneficiaryScreen(b.id, b.headName)) },
                                onReject = { showRejectDialog = true },
                                onAid = { navigator.push(AidDistributionScreen(b.id, b.headName)) },
                                onVisit = {
                                    navigator.push(
                                        VerificationVisitScreen(
                                            b.id, b.headName
                                        )
                                    )
                                })
                        }

                        item {
                            InfoCard("Personal Information", Icons.Default.Person) {
                                DetailRow("Age", b.headAge.toString())
                                DetailRow("Gender", b.headGender)
                                DetailRow("Occupation", b.headOccupation)
                                DetailRow("Education", b.headEducation)
                                DetailRow("Phone", b.phoneNumber)
                            }
                        }

                        item {
                            InfoCard("Address & Background", Icons.Default.LocationOn) {
                                DetailRow("Address", b.address)
                                DetailRow("Area Code", b.areaCode)
                                DetailRow("Nature", b.natureOfAddress)
                                b.natureOfRent?.let { DetailRow("Rent", it) }
                                DetailRow("Income", b.incomeSource)
                                b.diseaseInability?.let { DetailRow("Disease", it) }
                                DetailRow("Reason for Aid", b.reasonForAid)
                                DetailRow("Dependants", b.numberOfDependants.toString())
                            }
                        }

                        item {
                            AttachmentsSection(
                                attachments = b.attachments,
                                onAddClick = { showAddAttachmentDialog = true },
                                onDelete = { attachmentId -> viewModel.deleteAttachment(b.id, attachmentId) },
                                showDelete = user?.role == UserRole.APPROVER || user?.role == UserRole.SUPER_ADMIN
                            )
                        }

                        item {
                            DiscussionSection(
                                discussions = b.discussions,
                                onDelete = { noteId -> viewModel.deleteDiscussionNote(b.id, noteId) },
                                showDelete = user?.role == UserRole.APPROVER || user?.role == UserRole.SUPER_ADMIN,
                                isRecording = isRecording
                            )
                        }

                        if (b.status == BeneficiaryStatus.APPROVED) {
                            item {
                                InfoCard("Aid Approval Details", Icons.Default.CheckCircle) {
                                    b.natureOfAid?.let { DetailRow("Nature of Aid", it) }
                                    b.monthlyRation?.let { DetailRow("Monthly Ration", it) }
                                    b.packetCount?.let { DetailRow("Packet Count", it.toString()) }
                                    b.monetaryAidAmount?.let { DetailRow("Monetary Aid", it.toString()) }
                                    b.assignedMonitor?.let { DetailRow("Assigned Monitor", it) }
                                    b.approvalNotes?.let { DetailRow("Notes", it) }
                                }
                            }
                        }

                        if (b.status == BeneficiaryStatus.REJECTED) {
                            item {
                                InfoCard("Rejection Details", Icons.Default.Warning) {
                                    b.rejectionReason?.let { DetailRow("Reason", it) }
                                    b.rejectedBy?.let { DetailRow("Rejected By", it) }
                                }
                            }
                        }

                        if (b.status == BeneficiaryStatus.EDIT_REQUESTED) {
                            item {
                                InfoCard("Edit Request Details", Icons.Default.Edit) {
                                    b.editRequestNotes?.let { DetailRow("Requested Changes", it) }
                                }
                            }
                        }

                        if (b.familyMembers.isNotEmpty()) {
                            item {
                                SectionHeader("Family Members", Icons.Default.Face)
                            }
                            items(b.familyMembers) { member ->
                                FamilyMemberCard(member)
                            }
                        }

                        item {
                            InfoCard("Metadata", Icons.Default.Info) {
                                if (b.startMonth != null && b.startYear != null) {
                                    DetailRow("Start Date", "${b.startMonth}/${b.startYear}")
                                }
                                DetailRow("Onboarding Date", formatDate(b.onboardingDate))
                                DetailRow("Onboarded By", b.onboardedBy)
                                DetailRow("Device", b.deviceUsed)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Location",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${b.latitude}, ${b.longitude}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.End
                                        )
                                        if (b.latitude != 0.0 || b.longitude != 0.0) {
                                            IconButton(
                                                onClick = { openMaps(b.latitude, b.longitude, b.headName) },
                                                modifier = Modifier.size(32.dp).padding(start = 4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.LocationOn,
                                                    contentDescription = "Open in Maps",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (visits.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SectionHeader("Recent Visit", Icons.Default.Refresh)
                                    TextButton(onClick = { navigator.push(VisitHistoryScreen(b.headName, visits)) }) {
                                        Text("View All")
                                    }
                                }
                            }
                            item {
                                VisitCard(visits.first())
                            }
                        }

                        if (distributions.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SectionHeader("Last Aid", Icons.AutoMirrored.Filled.List)
                                    TextButton(onClick = { navigator.push(AidHistoryScreen(b.headName, distributions)) }) {
                                        Text("View All")
                                    }
                                }
                            }
                            item {
                                DistributionCard(distributions.first())
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        if (isUploading) {
            LoadingOverlay("Uploading file...")
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Beneficiary") },
                text = { Text("Are you sure you want to delete this beneficiary record? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBeneficiary(beneficiaryId) {
                                showDeleteDialog = false
                                navigator.pop()
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                })
        }

        if (showRejectDialog) {
            AlertDialog(
                onDismissRequest = { showRejectDialog = false },
                title = { Text("Reject Beneficiary") },
                text = {
                    Column {
                        Text("Please provide a reason for rejection:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = rejectionReason,
                            onValueChange = { rejectionReason = it },
                            label = { Text("Reason") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.rejectBeneficiary(beneficiaryId, rejectionReason) {
                                showRejectDialog = false
                                navigator.pop()
                            }
                        },
                        enabled = rejectionReason.isNotBlank(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reject")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRejectDialog = false }) {
                        Text("Cancel")
                    }
                })
        }

        if (showAddAttachmentDialog) {
            AlertDialog(
                onDismissRequest = { showAddAttachmentDialog = false },
                title = { Text("Add Attachment") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Select a document or file to attach to this beneficiary's record.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        
                        OutlinedTextField(
                            value = attachmentName,
                            onValueChange = { attachmentName = it },
                            label = { Text("Document Name (e.g. Identity Proof)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { 
                                    scope.launch {
                                        val picked = getFilePicker().takePhoto()
                                        if (picked != null) {
                                            viewModel.uploadAndAddAttachment(beneficiaryId, attachmentName, picked)
                                            showAddAttachmentDialog = false
                                            attachmentName = ""
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant, 
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.AccountBox, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Camera", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { 
                                    scope.launch {
                                        val picked = getFilePicker().pickImageOrPdf()
                                        if (picked != null) {
                                            viewModel.uploadAndAddAttachment(beneficiaryId, attachmentName, picked)
                                            showAddAttachmentDialog = false
                                            attachmentName = ""
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer, 
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Add, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Files/Gallery", fontSize = 12.sp)
                            }
                        }

                        if (attachmentUrl.isNotEmpty()) {
                            Text(
                                "File ready to upload: $attachmentName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                confirmButton = {
                    // Selection is handled by the "Select & Upload" button above
                },
                dismissButton = {
                    TextButton(onClick = { showAddAttachmentDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showPhotoDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoDialog = false },
                title = { Text(if (beneficiary?.photoUrl.isNullOrBlank()) "Add Profile Photo" else "Update Profile Photo") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "Capture or select a profile photo for the beneficiary.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { 
                                    scope.launch {
                                        val picked = getFilePicker().takePhoto()
                                        if (picked != null) {
                                            viewModel.uploadAndSetPhoto(beneficiaryId, picked)
                                            showPhotoDialog = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant, 
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.AccountBox, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Camera", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { 
                                    scope.launch {
                                        val picked = getFilePicker().pickImage()
                                        if (picked != null) {
                                            viewModel.uploadAndSetPhoto(beneficiaryId, picked)
                                            showPhotoDialog = false
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant, 
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(Icons.Default.AccountCircle, null)
                                Spacer(Modifier.width(4.dp))
                                Text("Gallery", fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    // Selection is handled by the "Pick Photo" button above
                },
                dismissButton = {
                    TextButton(onClick = { showPhotoDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

}

@Composable
internal fun DistributionCard(dist: AidDistribution) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(formatDate(dist.date), style = MaterialTheme.typography.labelMedium)
                    Text(dist.natureOfAid, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (dist.aidAmount > 0) Text("₹ ${dist.aidAmount}", fontWeight = FontWeight.Bold)
                    if (dist.packetCount > 0) Text("${dist.packetCount} Packets", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (dist.distributionLocationLat != 0.0 || dist.distributionLocationLng != 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { openMaps(dist.distributionLocationLat, dist.distributionLocationLng, "Aid: ${dist.beneficiaryName}") },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Location", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dateTime.dayOfMonth}/${dateTime.month.number}/${dateTime.year}"
}

@Composable
internal fun HeaderSection(b: Beneficiary, onPhotoClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onPhotoClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (b.photoUrl.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            var isLoading by remember { mutableStateOf(true) }
                            AsyncImage(
                                model = b.photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                onLoading = { isLoading = true },
                                onSuccess = { isLoading = false },
                                onError = { 
                                    isLoading = false
                                    println("COIL_ERROR: ${it.result.throwable.message}")
                                }
                            )
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    } else {
                        Text(
                            text = b.headName.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(bottom = 4.dp),
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        b.headName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    StatusBadge(b.status)
                }
            }
        }
    }
}

@Composable
internal fun AttachmentsSection(
    attachments: List<Attachment>,
    onAddClick: () -> Unit,
    onDelete: (String) -> Unit,
    showDelete: Boolean
) {
    InfoCard("Attachments & Documents", Icons.Default.Add) {
        if (attachments.isEmpty()) {
            Text(
                "No attachments added yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            attachments.forEach { attachment ->
                AttachmentRow(attachment, onDelete = { onDelete(attachment.id) }, showDelete = showDelete)
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        
        Button(
            onClick = onAddClick,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add Attachment")
        }
    }
}

@Composable
internal fun AttachmentRow(attachment: Attachment, onDelete: () -> Unit, showDelete: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openUrl(attachment.url) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (attachment.url.lowercase().endsWith(".pdf")) Icons.Default.Info else Icons.Default.Info, 
            null, 
            modifier = Modifier.size(24.dp), 
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                attachment.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Uploaded by ${attachment.uploadedBy} on ${formatDate(attachment.timestamp)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Row {
            IconButton(onClick = { shareUrl(attachment.url, attachment.name) }) {
                Icon(Icons.Default.Share, "Share", tint = MaterialTheme.colorScheme.primary)
            }
            if (showDelete) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
internal fun DiscussionSection(
    discussions: List<DiscussionNote>,
    onDelete: (String) -> Unit,
    showDelete: Boolean,
    isRecording: Boolean = false
) {
    InfoCard("Discussion (Voice Notes)", Icons.Default.Call) {
        if (discussions.isEmpty()) {
            Text(
                "No discussion recorded yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            discussions.sortedByDescending { it.timestamp }.forEach { note ->
                DiscussionRow(note, onDelete = { onDelete(note.id) }, showDelete = showDelete)
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        if (isRecording) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Recording in progress...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
internal fun DiscussionRow(note: DiscussionNote, onDelete: () -> Unit, showDelete: Boolean) {
    val player = remember { getAudioPlayer() }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            player.stop()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            if (isPlaying) {
                player.pause()
                isPlaying = false
            } else {
                player.play(note.audioUrl)
                isPlaying = true
                player.setCompletionListener { isPlaying = false }
            }
        }) {
            Icon(
                if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(
                "Voice Note - ${note.durationMs / 1000}s",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                "By ${note.senderName} on ${formatDate(note.timestamp)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        if (showDelete) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
internal fun ActionButtons(
    b: Beneficiary,
    user: User?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onAid: () -> Unit,
    onVisit: () -> Unit
) {
    val isEmployee = user?.role == UserRole.EMPLOYEE
    val isApprover = user?.role == UserRole.APPROVER
    val isSuperAdmin = user?.role == UserRole.SUPER_ADMIN

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Edit/Delete Section - Restricted to Employee/Admin
        if (isEmployee || isSuperAdmin) {
            if (b.status == BeneficiaryStatus.PENDING_APPROVAL || 
                b.status == BeneficiaryStatus.REAPPROVAL_PENDING ||
                b.status == BeneficiaryStatus.MISUSE_REPORTED ||
                b.status == BeneficiaryStatus.EDIT_REQUESTED ||
                b.status == BeneficiaryStatus.DRAFT) {
                Row(
                    modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit")
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
            }
        }

        // Approval Section - Restricted to Approver/Admin
        if (isApprover || isSuperAdmin) {
            if (b.status == BeneficiaryStatus.PENDING_APPROVAL || 
                b.status == BeneficiaryStatus.REAPPROVAL_PENDING ||
                b.status == BeneficiaryStatus.MISUSE_REPORTED ||
                b.status == BeneficiaryStatus.EDIT_REQUESTED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            when (b.status) {
                                BeneficiaryStatus.REAPPROVAL_PENDING -> "Re-approve"
                                BeneficiaryStatus.MISUSE_REPORTED -> "Clear & Approve"
                                BeneficiaryStatus.EDIT_REQUESTED -> "Update & Approve"
                                else -> "Approve"
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reject")
                    }
                }
            }
        }

        // Aid/Visit Section - Restricted to Employee/Admin
        if (isEmployee || isSuperAdmin) {
            if (b.status == BeneficiaryStatus.APPROVED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAid,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Give Aid")
                    }

                    Button(
                        onClick = onVisit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify Visit")
                    }
                }
            }
        }
    }
}

@Composable
internal fun InfoCard(
    title: String, icon: ImageVector, content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            content()
        }
    }
}

@Composable
internal fun FamilyMemberCard(member: FamilyMember) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    member.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape
                ) {
                    Text(
                        text = member.relation,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Age: ${member.age} | Gender: ${member.gender}",
                style = MaterialTheme.typography.bodySmall
            )
            Text("Occupation: ${member.occupation}", style = MaterialTheme.typography.bodySmall)
            member.diseaseInability?.let {
                if (it.isNotBlank()) Text(
                    "Disease: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
internal fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 0.dp, max = 200.dp)
        )
    }
}

@Composable
internal fun StatusBadge(status: BeneficiaryStatus) {
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
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
            Text(
                text = when (status) {
                    BeneficiaryStatus.PENDING_APPROVAL -> "PENDING"
                    else -> status.name.replace("_", " ")
                },
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview
@Composable
fun HeaderSectionPreview() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            HeaderSection(PreviewMocks.mockBeneficiary)
        }
    }
}

@Preview
@Composable
fun ActionButtonsPreview() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            ActionButtons(
                b = PreviewMocks.mockBeneficiary,
                user = PreviewMocks.mockUser,
                onEdit = {},
                onDelete = {},
                onApprove = {},
                onReject = {},
                onAid = {},
                onVisit = {})
        }
    }
}

@Preview
@Composable
fun FamilyMemberCardPreview() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            FamilyMemberCard(PreviewMocks.mockBeneficiary.familyMembers.first())
        }
    }
}

@Preview
@Composable
fun SectionHeaderPreview() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            SectionHeader("Personal Information", Icons.Default.Person)
        }
    }
}

@Preview
@Composable
fun InfoCardPreview() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            InfoCard("Personal Information", Icons.Default.Person) {
                DetailRow("Age", "45")
                DetailRow("Gender", "Male")
            }
        }
    }
}

@Preview
@Composable
fun DetailRowPreview() {
    MaterialTheme {
        Box(Modifier.padding(16.dp).background(MaterialTheme.colorScheme.surface)) {
            DetailRow("Label", "2205, Growmore Emerald, Malwani, Malad, Mumbai 400095")
        }
    }
}

@Preview
@Composable
fun StatusBadgePreview() {
    MaterialTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(BeneficiaryStatus.PENDING_APPROVAL)
            StatusBadge(BeneficiaryStatus.APPROVED)
            StatusBadge(BeneficiaryStatus.REJECTED)
        }
    }
}

class BeneficiaryDetailViewModel(
    private val repository: BeneficiaryRepository,
    private val authRepository: AuthRepository,
    private val visitRepository: VisitRepository,
    private val aidRepository: AidRepository,
    private val storageRepository: StorageRepository
) : ScreenModel {
    private val _beneficiary = MutableStateFlow<Beneficiary?>(null)
    val beneficiary: StateFlow<Beneficiary?> = _beneficiary.asStateFlow()

    private val _visits = MutableStateFlow<List<VerificationVisit>>(emptyList())
    val visits: StateFlow<List<VerificationVisit>> = _visits.asStateFlow()

    private val _aidDistributions = MutableStateFlow<List<AidDistribution>>(emptyList())
    val aidDistributions: StateFlow<List<AidDistribution>> = _aidDistributions.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    val currentUser = authRepository.currentUser.stateIn(
            screenModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    fun loadBeneficiary(id: String) {
        screenModelScope.launch {
            repository.getBeneficiaryById(id).collect {
                _beneficiary.value = it
            }
        }
        screenModelScope.launch {
            visitRepository.getVisits().collect { allVisits ->
                _visits.value = allVisits.filter { it.beneficiaryId == id }.sortedByDescending { it.date }
            }
        }
        screenModelScope.launch {
            aidRepository.getDistributionsByBeneficiary(id).collect { distributions ->
                _aidDistributions.value = distributions.sortedByDescending { it.date }
            }
        }
    }

    fun deleteBeneficiary(id: String, onDeleted: () -> Unit) {
        screenModelScope.launch {
            val result = repository.deleteBeneficiary(id)
            if (result.isSuccess) {
                onDeleted()
            }
        }
    }

    fun rejectBeneficiary(id: String, reason: String, onRejected: () -> Unit) {
        screenModelScope.launch {
            val userId = currentUser.value?.userId ?: ""
            val result = repository.rejectBeneficiary(id, userId, reason)
            if (result.isSuccess) {
                onRejected()
            }
        }
    }

    fun updatePhoto(id: String, url: String) {
        screenModelScope.launch {
            repository.updatePhoto(id, url)
        }
    }

    fun addAttachment(id: String, name: String, url: String) {
        screenModelScope.launch {
            val user = currentUser.value
            val now = Clock.System.now().toEpochMilliseconds()
            val attachment = Attachment(
                id = "ATT_$now",
                name = name,
                url = url,
                timestamp = now,
                uploadedBy = user?.fullName ?: "Unknown"
            )
            repository.addAttachment(id, attachment)
        }
    }

    fun uploadAndAddAttachment(beneficiaryId: String, name: String, data: ByteArray) {
        screenModelScope.launch {
            _isUploading.value = true
            storageRepository.uploadFile("beneficiaries/$beneficiaryId", data, name).onSuccess { url ->
                addAttachment(beneficiaryId, name, url)
                _isUploading.value = false
            }.onFailure {
                _isUploading.value = false
            }
        }
    }

    fun deleteAttachment(beneficiaryId: String, attachmentId: String) {
        screenModelScope.launch {
            repository.deleteAttachment(beneficiaryId, attachmentId)
        }
    }

    fun uploadAndAddDiscussionNote(beneficiaryId: String, audioData: ByteArray, durationMs: Long) {
        screenModelScope.launch {
            _isUploading.value = true
            val fileName = "discussion_${Clock.System.now().toEpochMilliseconds()}.m4a"
            storageRepository.uploadFile("beneficiaries/$beneficiaryId/discussion", audioData, fileName).onSuccess { url ->
                val user = currentUser.value
                val now = Clock.System.now().toEpochMilliseconds()
                val note = DiscussionNote(
                    id = "NOTE_$now",
                    audioUrl = url,
                    durationMs = durationMs,
                    senderId = user?.userId ?: "",
                    senderName = user?.fullName ?: "Unknown",
                    timestamp = now
                )
                repository.addDiscussionNote(beneficiaryId, note)
                _isUploading.value = false
            }.onFailure {
                _isUploading.value = false
            }
        }
    }

    fun deleteDiscussionNote(beneficiaryId: String, noteId: String) {
        screenModelScope.launch {
            repository.deleteDiscussionNote(beneficiaryId, noteId)
        }
    }

    fun uploadAndSetPhoto(beneficiaryId: String, data: ByteArray) {
        screenModelScope.launch {
            _isUploading.value = true
            storageRepository.uploadPhoto(beneficiaryId, data).onSuccess { url ->
                updatePhoto(beneficiaryId, url)
                _isUploading.value = false
            }.onFailure {
                _isUploading.value = false
            }
        }
    }
}
