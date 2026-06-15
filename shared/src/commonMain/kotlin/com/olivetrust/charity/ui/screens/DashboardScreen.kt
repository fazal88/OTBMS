package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.olivetrust.charity.ui.previews.PreviewMocks
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.UserRole
import com.olivetrust.charity.domain.model.User
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import kotlinx.datetime.*
import kotlin.time.Clock

class DashboardScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DashboardViewModel>()
        val user by viewModel.currentUser.collectAsState()
        val stats by viewModel.stats.collectAsState()
        val isRefreshing by viewModel.isRefreshing.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        var showProfileDialog by remember { mutableStateOf(false) }

        LaunchedEffect(user) {
            if (user == null) {
                navigator.replaceAll(LoginScreen())
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Olive Trust", fontWeight = FontWeight.ExtraBold) },
                    actions = {
                        if (user?.role == UserRole.APPROVER || user?.role == UserRole.SUPER_ADMIN) {
                            IconButton(onClick = { navigator.push(EmployeeManagementScreen()) }) {
                                Icon(Icons.Default.AccountCircle, contentDescription = "Employees")
                            }
                        }
                        IconButton(onClick = { showProfileDialog = true }) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (user?.fullName?.take(1) ?: "U").uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (user?.role == UserRole.EMPLOYEE) {
                    ExtendedFloatingActionButton(
                        onClick = { navigator.push(OnboardingScreen()) },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("New Beneficiary") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.padding(padding)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(span = { GridItemSpan(2) }) {
                        WelcomeHeader(user?.fullName ?: "User")
                    }

                    // View All Buttons
                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { navigator.push(BeneficiaryListScreen()) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.Face, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Beneficiaries", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { navigator.push(VisitListScreen()) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Visits", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { navigator.push(AidListScreen()) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.List, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Aid", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = { navigator.push(ApprovalListScreen()) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Approvals", fontSize = 10.sp)
                            }
                        }
                    }

                    item(span = { GridItemSpan(2) }) {
                        SectionLabel("Key Metrics")
                    }
                    item {
                        MainStatCard(
                            label = "Approved",
                            value = stats.approvedBeneficiaries.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF4CAF50),
                            onClick = { navigator.push(BeneficiaryListScreen(BeneficiaryFilters(status = BeneficiaryStatus.APPROVED))) }
                        )
                    }
                    item {
                        MainStatCard(
                            label = "Visits",
                            value = stats.totalVisits.toString(),
                            icon = Icons.Default.LocationOn,
                            color = Color(0xFF2196F3),
                            onClick = { navigator.push(VisitListScreen()) }
                        )
                    }
                    item(span = { GridItemSpan(2) }) {
                        WideStatCard(
                            label = "Monthly Aid Distributions",
                            value = stats.monthlyAidDistributed.toString(),
                            icon = Icons.AutoMirrored.Filled.List,
                            color = MaterialTheme.colorScheme.tertiary,
                            subtitle = "For ${getCurrentMonthName()}",
                            onClick = { navigator.push(AidListScreen()) }
                        )
                    }

                    item(span = { GridItemSpan(2) }) {
                        SectionLabel("Attention Required")
                    }
                    
                    item {
                        PendingCard(
                            label = "Onboarding",
                            value = stats.pendingOnboarding.toString(),
                            icon = Icons.Default.Person,
                            color = Color(0xFFFF9800),
                            onClick = { navigator.push(BeneficiaryListScreen(BeneficiaryFilters(status = BeneficiaryStatus.PENDING_APPROVAL))) }
                        )
                    }
                    item {
                        PendingCard(
                            label = "Edit Requests",
                            value = stats.pendingEdits.toString(),
                            icon = Icons.Default.Edit,
                            color = Color(0xFF9C27B0),
                            onClick = { navigator.push(BeneficiaryListScreen(BeneficiaryFilters(status = BeneficiaryStatus.EDIT_REQUESTED))) }
                        )
                    }
                    item {
                        PendingCard(
                            label = "Misuse Reports",
                            value = stats.misuseReports.toString(),
                            icon = Icons.Default.Warning,
                            color = Color(0xFFF44336),
                            onClick = { navigator.push(BeneficiaryListScreen(BeneficiaryFilters(status = BeneficiaryStatus.MISUSE_REPORTED))) }
                        )
                    }
                    item {
                        PendingCard(
                            label = "Reapprovals",
                            value = stats.pendingReapprovals.toString(),
                            icon = Icons.Default.Refresh,
                            color = Color(0xFF607D8B),
                            onClick = { navigator.push(BeneficiaryListScreen(BeneficiaryFilters(status = BeneficiaryStatus.REAPPROVAL_PENDING))) }
                        )
                    }

                    item(span = { GridItemSpan(2) }) {
                        Spacer(Modifier.height(80.dp))
                    }
                }
            }
        }

        if (showProfileDialog) {
            ProfileDialog(
                user = user,
                onDismiss = { showProfileDialog = false },
                onLogout = {
                    showProfileDialog = false
                    viewModel.logout()
                },
                onUpdateProfile = { name, phone ->
                    viewModel.updateProfile(name, phone)
                }
            )
        }
    }
}

@Composable
fun WelcomeHeader(name: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = "Welcome back,",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = name,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun MainStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, modifier = Modifier.size(24.dp), tint = color)
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun WideStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))
                Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = color)
            }
            Icon(icon, null, modifier = Modifier.size(48.dp), tint = color.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun PendingCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (value != "0") color.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (value != "0") color.copy(alpha = 0.05f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (value != "0") color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(16.dp), tint = if (value != "0") color else MaterialTheme.colorScheme.outline)
            }
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (value != "0") color else MaterialTheme.colorScheme.onSurface)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
    }
}

private fun getCurrentMonthName(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return now.month.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

@Composable
internal fun ProfileDialog(
    user: User?,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onUpdateProfile: (String, String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var name by remember(user) { mutableStateOf(user?.fullName ?: "") }
    var phone by remember(user) { mutableStateOf(user?.mobileNumber ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (user?.fullName?.take(1) ?: "U").uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "My Profile",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Person, null) }
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Phone, null) }
                    )
                } else {
                    ProfileInfoRow(Icons.Default.Person, "Name", user?.fullName ?: "N/A")
                    ProfileInfoRow(Icons.Default.Phone, "Phone", user?.mobileNumber ?: "N/A")
                    ProfileInfoRow(Icons.Default.Info, "Role", user?.role?.name ?: "N/A")
                }
                
                if (user?.role != UserRole.SUPER_ADMIN) {
                    Button(
                        onClick = { 
                            if (isEditing) {
                                onUpdateProfile(name, phone)
                                isEditing = false
                            } else {
                                isEditing = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(if (isEditing) Icons.Default.Check else Icons.Default.Edit, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isEditing) "Save Changes" else "Edit Profile")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onLogout,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Logout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
