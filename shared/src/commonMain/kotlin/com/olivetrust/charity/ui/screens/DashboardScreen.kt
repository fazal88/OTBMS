package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.tooling.preview.Preview
import com.olivetrust.charity.ui.previews.PreviewMocks
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.domain.model.UserRole
import com.olivetrust.charity.domain.model.User

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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column {
                            Text(
                                text = "Hello,",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = user?.fullName ?: "User",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        StatCard(
                            label = "Total Beneficiaries",
                            value = stats.total.toString(),
                            icon = Icons.Default.Face,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { navigator.push(BeneficiaryListScreen()) }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SmallStatCard(
                                label = "Approved",
                                value = stats.active.toString(),
                                icon = Icons.Default.CheckCircle,
                                color = Color(0xFF4CAF50), // Green
                                modifier = Modifier.weight(1f)
                            )
                            SmallStatCard(
                                label = "Pending",
                                value = stats.pending.toString(),
                                icon = Icons.Default.Info,
                                color = Color(0xFFFF9800), // Orange
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SmallStatCard(
                                label = "Reapproval",
                                value = stats.reapproval.toString(),
                                icon = Icons.Default.Refresh,
                                color = Color(0xFF2196F3), // Blue
                                modifier = Modifier.weight(1f)
                            )
                            SmallStatCard(
                                label = "Edit Request",
                                value = stats.editRequested.toString(),
                                icon = Icons.Default.Edit,
                                color = Color(0xFF9C27B0), // Purple
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        SmallStatCard(
                            label = "Rejected",
                            value = stats.rejected.toString(),
                            icon = Icons.Default.Close,
                            color = Color(0xFFF44336) // Red
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
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
internal fun StatCard(
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
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }
            Surface(
                color = color,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun SmallStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
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

@Preview
@Composable
fun StatCardPreview() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            StatCard("Total Beneficiaries", "125", Icons.Default.Face, MaterialTheme.colorScheme.primary, {})
        }
    }
}

@Preview
@Composable
fun ProfileDialogPreview() {
    MaterialTheme {
        ProfileDialog(
            user = PreviewMocks.mockUser,
            onDismiss = {},
            onLogout = {},
            onUpdateProfile = { _, _ -> }
        )
    }
}
