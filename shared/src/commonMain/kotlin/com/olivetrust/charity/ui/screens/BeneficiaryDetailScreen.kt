package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.background
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
import com.olivetrust.charity.domain.model.*
import com.olivetrust.charity.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.*
import androidx.compose.foundation.lazy.items

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

        var showDeleteDialog by remember { mutableStateOf(false) }
        var showRejectDialog by remember { mutableStateOf(false) }
        var rejectionReason by remember { mutableStateOf("") }

        LaunchedEffect(beneficiaryId) {
            viewModel.loadBeneficiary(beneficiaryId)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Beneficiary Details", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    })
            }) { padding ->
            beneficiary?.let { b ->
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            HeaderSection(b)
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
                                DetailRow("Location", "${b.latitude}, ${b.longitude}")
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
    }

}

@Composable
internal fun VisitCard(visit: VerificationVisit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    formatDate(visit.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    visit.visitStatus.name.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (visit.visitStatus) {
                        VisitStatus.SUCCESSFUL -> Color(0xFF4CAF50)
                        VisitStatus.REAPPROVAL_REQUIRED -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
            visit.reapprovalReason?.let {
                Text("Note: $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
            visit.misuseReport?.let {
                Text("Report: ${it.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
internal fun DistributionCard(dist: AidDistribution) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formatDate(dist.date), style = MaterialTheme.typography.labelMedium)
                Text(dist.natureOfAid, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (dist.aidAmount > 0) Text("₹ ${dist.aidAmount}", fontWeight = FontWeight.Bold)
                if (dist.packetCount > 0) Text("${dist.packetCount} Packets", style = MaterialTheme.typography.bodySmall)
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
internal fun HeaderSection(b: Beneficiary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = b.headName.take(1).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (b.status == BeneficiaryStatus.PENDING_APPROVAL || 
                b.status == BeneficiaryStatus.REAPPROVAL_PENDING ||
                b.status == BeneficiaryStatus.MISUSE_REPORTED ||
                b.status == BeneficiaryStatus.EDIT_REQUESTED) {
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

        if (user?.role == UserRole.APPROVER || user?.role == UserRole.SUPER_ADMIN) {
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

@Composable
internal fun InfoCard(
    title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit
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
                text = status.name.replace("_", " "),
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
    private val aidRepository: AidRepository
) : ScreenModel {
    private val _beneficiary = MutableStateFlow<Beneficiary?>(null)
    val beneficiary: StateFlow<Beneficiary?> = _beneficiary.asStateFlow()

    private val _visits = MutableStateFlow<List<VerificationVisit>>(emptyList())
    val visits: StateFlow<List<VerificationVisit>> = _visits.asStateFlow()

    private val _aidDistributions = MutableStateFlow<List<AidDistribution>>(emptyList())
    val aidDistributions: StateFlow<List<AidDistribution>> = _aidDistributions.asStateFlow()

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
}
