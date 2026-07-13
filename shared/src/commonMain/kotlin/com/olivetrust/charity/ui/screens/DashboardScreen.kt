package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.rememberScrollState
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.UserRole
import com.olivetrust.charity.domain.model.User
import com.olivetrust.charity.domain.model.BeneficiaryStatus
import com.olivetrust.charity.domain.model.CollectionStatus
import com.olivetrust.charity.domain.model.DonationBoxStatus
import com.olivetrust.charity.ui.previews.PreviewMocks
import com.olivetrust.charity.ui.theme.OliveTheme
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.*
import kotlin.time.Clock
import kotlin.time.Instant

class DashboardScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DashboardViewModel>()
        val user by viewModel.currentUser.collectAsState()
        val stats by viewModel.stats.collectAsState()
        val isRefreshing by viewModel.isRefreshing.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(user) {
            if (user == null) {
                navigator.replaceAll(LoginScreen())
            }
        }

        DashboardContent(
            user = user,
            stats = stats,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            onLogout = { viewModel.logout() },
            onUpdateProfile = { name, phone -> viewModel.updateProfile(name, phone) },
            onNotificationsClick = { navigator.push(NotificationTopicsScreen()) },
            onEmployeesClick = { navigator.push(EmployeeManagementScreen()) },
            onOnboardingClick = { navigator.push(OnboardingScreen()) },
            onBeneficiaryStatusClick = { status ->
                navigator.push(BeneficiaryListScreen(BeneficiaryFilters(status = status)))
            },
            onVisitListClick = { navigator.push(VisitListScreen()) },
            onAidListClick = { navigator.push(AidListScreen()) },
            onDonationBoxStatusClick = { status ->
                navigator.push(DonationBoxListScreen(DonationBoxFilters(status = status)))
            },
            onDonationCollectionStatusClick = { status ->
                navigator.push(DonationCollectionListScreen(DonationCollectionFilters(status = status)))
            },
            onEventListClick = { navigator.push(EventListScreen()) },
            onViewAllDonationBoxesClick = { navigator.push(DonationBoxListScreen()) },
            onViewAllBeneficiariesClick = { navigator.push(BeneficiaryListScreen()) },
            onViewAllCollectionsClick = { navigator.push(DonationCollectionListScreen()) },
            onViewAllAidHistoryClick = { navigator.push(AidListScreen()) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    user: User?,
    stats: DashboardStats,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onNotificationsClick: () -> Unit,
    onEmployeesClick: () -> Unit,
    onOnboardingClick: () -> Unit,
    onBeneficiaryStatusClick: (BeneficiaryStatus) -> Unit,
    onVisitListClick: () -> Unit,
    onAidListClick: () -> Unit,
    onDonationBoxStatusClick: (DonationBoxStatus) -> Unit,
    onDonationCollectionStatusClick: (CollectionStatus) -> Unit,
    onEventListClick: () -> Unit,
    onViewAllDonationBoxesClick: () -> Unit,
    onViewAllBeneficiariesClick: () -> Unit,
    onViewAllCollectionsClick: () -> Unit,
    onViewAllAidHistoryClick: () -> Unit
) {
    var showProfileDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Olive Trust",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                },
                actions = {
                    if (user?.role == UserRole.APPROVER || user?.role == UserRole.SUPER_ADMIN) {
                        IconButton(onClick = onNotificationsClick) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                        IconButton(onClick = onEmployeesClick) {
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
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            if (user?.role == UserRole.EMPLOYEE) {
                ExtendedFloatingActionButton(
                    onClick = onOnboardingClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Beneficiary") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(span = { GridItemSpan(2) }) {
                    WelcomeHeader(user?.fullName ?: "User")
                }

                // View All Buttons - Quick Actions
                item(span = { GridItemSpan(2) }) {
                    QuickActionsRow(
                        user = user,
                        onDonationBoxListClick = onViewAllDonationBoxesClick,
                        onDonationCollectionListClick = onViewAllCollectionsClick,
                        onBeneficiaryListClick = onViewAllBeneficiariesClick,
                        onEventListClick = onEventListClick,
                        onVisitListClick = onVisitListClick,
                        onAidListClick = onViewAllAidHistoryClick
                    )
                }

                val role = user?.role
                val showDonation = role == UserRole.APPROVER || role == UserRole.COLLECTOR
                val showBeneficiary = role == UserRole.APPROVER || role == UserRole.EMPLOYEE

                if (showBeneficiary) {
                    item(span = { GridItemSpan(2) }) {
                        SectionLabel("Beneficiary Metrics")
                    }
                    item {
                        MainStatCard(
                            label = "Approved",
                            value = stats.approvedBeneficiaries.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF386B1D),
                            onClick = { onBeneficiaryStatusClick(BeneficiaryStatus.APPROVED) }
                        )
                    }
                    item {
                        MainStatCard(
                            label = "Visits (${getCurrentMonthName().take(3)})",
                            value = stats.monthlyVisits.toString(),
                            icon = Icons.Default.LocationOn,
                            color = Color(0xFF2E5B8E),
                            onClick = onVisitListClick
                        )
                    }
                    item(span = { GridItemSpan(2) }) {
                        WideStatCard(
                            label = "Aid Distributed",
                            value = stats.monthlyAidDistributed.toString(),
                            icon = Icons.AutoMirrored.Filled.List,
                            color = MaterialTheme.colorScheme.tertiary,
                            subtitle = "Performance for ${getCurrentMonthName()}",
                            onClick = onAidListClick
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
                            color = Color(0xFFB36200),
                            onClick = { onBeneficiaryStatusClick(BeneficiaryStatus.PENDING_APPROVAL) }
                        )
                    }
                    item {
                        PendingCard(
                            label = "Edit Requests",
                            value = stats.pendingEdits.toString(),
                            icon = Icons.Default.Edit,
                            color = Color(0xFF7B2E8E),
                            onClick = { onBeneficiaryStatusClick(BeneficiaryStatus.EDIT_REQUESTED) }
                        )
                    }
                    item {
                        PendingCard(
                            label = "Misuse Reports",
                            value = stats.misuseReports.toString(),
                            icon = Icons.Default.Warning,
                            color = Color(0xFFB3261E),
                            onClick = { onBeneficiaryStatusClick(BeneficiaryStatus.MISUSE_REPORTED) }
                        )
                    }
                    item {
                        PendingCard(
                            label = "Expired",
                            value = stats.expiredBeneficiaries.toString(),
                            icon = Icons.Default.Info,
                            color = Color(0xFF44483D),
                            onClick = { onBeneficiaryStatusClick(BeneficiaryStatus.EXPIRED) }
                        )
                    }
                }

                if (showDonation) {
                    item(span = { GridItemSpan(2) }) {
                        SectionLabel("Donation Boxes")
                    }
                    item {
                        MainStatCard(
                            label = "Active\nBoxes",
                            value = stats.activeDonationBoxes.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF386B1D),
                            onClick = { onDonationBoxStatusClick(DonationBoxStatus.ACTIVE) }
                        )
                    }
                    item {
                        MainStatCard(
                            label = "Today's\nCollections",
                            value = stats.collectionsToday.toString(),
                            icon = Icons.Default.ShoppingCart,
                            color = Color(0xFF2E5B8E),
                            onClick = onViewAllDonationBoxesClick
                        )
                    }
                    if (role == UserRole.APPROVER) {
                        item(span = { GridItemSpan(2) }) {
                            SectionLabel("Treasury & Finance")
                        }
                        item(span = { GridItemSpan(2) }) {
                            TreasuryStatCard(
                                label = "Total Collected",
                                value = "₹${formatCurrency(stats.totalAmountCollected)}",
                                icon = Icons.Default.ShoppingCart,
                                color = MaterialTheme.colorScheme.primary,
                                onClick = onViewAllDonationBoxesClick
                            )
                        }
                        item(span = { GridItemSpan(2) }) {
                            TreasuryStatCard(
                                label = "Total Received",
                                value = "₹${formatCurrency(stats.totalAmountReceived)}",
                                icon = Icons.Default.CheckCircle,
                                color = Color(0xFF386B1D),
                                onClick = onViewAllDonationBoxesClick
                            )
                        }
                    }

                    if (stats.pendingDonationBoxes > 0 || stats.reportedIssues > 0 || stats.pendingCollections > 0) {
                        item(span = { GridItemSpan(2) }) {
                            SectionLabel("Donation Box Attention")
                        }
                        if (stats.pendingDonationBoxes > 0) {
                            item {
                                PendingCard(
                                    label = "New Boxes",
                                    value = stats.pendingDonationBoxes.toString(),
                                    icon = Icons.Default.Add,
                                    color = Color(0xFFB36200),
                                    onClick = { onDonationBoxStatusClick(DonationBoxStatus.PENDING_APPROVAL) }
                                )
                            }
                        }
                        if (stats.pendingCollections > 0) {
                            item {
                                PendingCard(
                                    label = "Pending Collections",
                                    value = stats.pendingCollections.toString(),
                                    icon = Icons.Default.ShoppingCart,
                                    color = Color(0xFFB36200),
                                    onClick = { onDonationCollectionStatusClick(CollectionStatus.PENDING) }
                                )
                            }
                        }
                        if (stats.reportedIssues > 0) {
                            item {
                                PendingCard(
                                    label = "Reported Issues",
                                    value = stats.reportedIssues.toString(),
                                    icon = Icons.Default.Warning,
                                    color = Color(0xFFB3261E),
                                    onClick = onViewAllDonationBoxesClick
                                )
                            }
                        }
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    Spacer(Modifier.height(80.dp))
                }

                if (role == UserRole.APPROVER || role == UserRole.SUPER_ADMIN) {
                    item(span = { GridItemSpan(2) }) {
                        SectionLabel("User Management")
                    }
                    item {
                        MainStatCard(
                            label = "Total Users",
                            value = stats.totalEmployees.toString(),
                            icon = Icons.Default.AccountCircle,
                            color = Color(0xFF6750A4),
                            onClick = onEmployeesClick
                        )
                    }
                    item {
                        MainStatCard(
                            label = "Active Users",
                            value = stats.activeEmployees.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF386B1D),
                            onClick = onEmployeesClick
                        )
                    }
                    if (stats.pendingDeviceApprovals > 0) {
                        item(span = { GridItemSpan(2) }) {
                            PendingCard(
                                label = "Device Approval Pending",
                                value = stats.pendingDeviceApprovals.toString(),
                                icon = Icons.Default.Info,
                                color = Color(0xFFB36200),
                                onClick = onEmployeesClick
                            )
                        }
                    }
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
                onLogout()
            },
            onUpdateProfile = { name, phone ->
                onUpdateProfile(name, phone)
            }
        )
    }
}

@Composable
fun QuickActionsRow(
    user: User?,
    onDonationBoxListClick: () -> Unit,
    onDonationCollectionListClick: () -> Unit,
    onBeneficiaryListClick: () -> Unit,
    onEventListClick: () -> Unit,
    onVisitListClick: () -> Unit,
    onAidListClick: () -> Unit
) {
    val role = user?.role
    val showDonation = role == UserRole.APPROVER || role == UserRole.COLLECTOR
    val showBeneficiary = role == UserRole.APPROVER || role == UserRole.EMPLOYEE

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showDonation) {
            AssistChip(
                onClick = onDonationBoxListClick,
                label = { Text("Donation Boxes") },
                leadingIcon = {
                    Icon(
                        Icons.Default.ShoppingCart,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
            AssistChip(
                onClick = onDonationCollectionListClick,
                label = { Text("Collections") },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
        }
        if (showBeneficiary) {
            AssistChip(
                onClick = onBeneficiaryListClick,
                label = { Text("Beneficiaries") },
                leadingIcon = { Icon(Icons.Default.Face, null, modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(12.dp)
            )
            AssistChip(
                onClick = onEventListClick,
                label = { Text("Events") },
                leadingIcon = {
                    Icon(
                        Icons.Default.DateRange,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
            AssistChip(
                onClick = onVisitListClick,
                label = { Text("Visits") },
                leadingIcon = {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
            AssistChip(
                onClick = onAidListClick,
                label = { Text("Aid History") },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun WelcomeHeader(name: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = "Welcome back,",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = name,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 8.dp),
        letterSpacing = 1.sp
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, modifier = Modifier.size(20.dp), tint = color)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = color.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                value,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun TreasuryStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = color,
        tonalElevation = 8.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(28.dp), tint = Color.White)
                }
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    return if (amount >= 1000000) {
        "${(amount / 1000000.0).toString().take(4)}M"
    } else if (amount >= 1000) {
        "${(amount / 1000.0).toString().take(4)}K"
    } else {
        amount.toInt().toString()
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
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    value,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = color
                )
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(32.dp), tint = color.copy(alpha = 0.4f))
            }
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
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (value != "0") color.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (value != "0") color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant.copy(
                alpha = 0.5f
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (value != "0") color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = if (value != "0") color else MaterialTheme.colorScheme.outline
                )
            }
            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (value != "0") color else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun getCurrentMonthName(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return now.month.name.lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
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
        shape = RoundedCornerShape(32.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (user?.fullName?.take(1) ?: "U").uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview
@Composable
fun DashboardPreview() {
    OliveTheme {
        DashboardContent(
            user = PreviewMocks.mockUser,
            stats = DashboardStats(
                approvedBeneficiaries = 120,
                monthlyAidDistributed = 45,
                monthlyVisits = 30,
                pendingOnboarding = 5,
                pendingEdits = 3,
                misuseReports = 1,
                expiredBeneficiaries = 2,
                totalAmountCollected = 150000.0,
                totalAmountReceived = 145000.0,
                activeDonationBoxes = 10,
                collectionsToday = 5,
                pendingDonationBoxes = 2,
                pendingCollections = 4,
                reportedIssues = 1,
                totalEmployees = 15,
                activeEmployees = 12,
                pendingDeviceApprovals = 2
            ),
            isRefreshing = false,
            onRefresh = {},
            onLogout = {},
            onUpdateProfile = { _, _ -> },
            onNotificationsClick = {},
            onEmployeesClick = {},
            onOnboardingClick = {},
            onBeneficiaryStatusClick = {},
            onVisitListClick = {},
            onAidListClick = {},
            onDonationBoxStatusClick = {},
            onDonationCollectionStatusClick = {},
            onEventListClick = {},
            onViewAllDonationBoxesClick = {},
            onViewAllBeneficiariesClick = {},
            onViewAllCollectionsClick = {},
            onViewAllAidHistoryClick = {}
        )
    }
}
