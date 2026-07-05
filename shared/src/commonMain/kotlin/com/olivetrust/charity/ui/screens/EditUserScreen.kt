package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.olivetrust.charity.data.util.HashUtil
import com.olivetrust.charity.domain.model.User
import com.olivetrust.charity.domain.model.UserRole
import com.olivetrust.charity.domain.model.UserStatus
import kotlin.time.Clock

class EditUserScreen(private val userToEdit: User? = null) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<EmployeeManagementViewModel>()
        val navigator = LocalNavigator.currentOrThrow
        val focusManager = LocalFocusManager.current

        var fullName by remember { mutableStateOf(userToEdit?.fullName ?: "") }
        var username by remember { mutableStateOf(userToEdit?.username ?: "") }
        var password by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(userToEdit?.role ?: UserRole.EMPLOYEE) }
        var status by remember { mutableStateOf(userToEdit?.status ?: UserStatus.ACTIVE) }
        var mobileNumber by remember { mutableStateOf(userToEdit?.mobileNumber ?: "") }
        var employeeCode by remember { mutableStateOf(userToEdit?.employeeCode ?: "") }

        fun saveUser() {
            val now = Clock.System.now().toEpochMilliseconds()
            val finalUser = if (userToEdit == null) {
                User(
                    userId = "U_$now",
                    username = username,
                    fullName = fullName,
                    mobileNumber = mobileNumber,
                    role = role,
                    status = status,
                    employeeCode = employeeCode,
                    passwordHash = HashUtil.hashPassword(password),
                    createdAt = now
                )
            } else {
                userToEdit.copy(
                    fullName = fullName,
                    mobileNumber = mobileNumber,
                    role = role,
                    status = status,
                    employeeCode = employeeCode,
                    passwordHash = if (password.isNotEmpty()) HashUtil.hashPassword(password) else userToEdit.passwordHash
                )
            }

            if (userToEdit == null) {
                viewModel.createEmployee(finalUser, finalUser.passwordHash)
            } else {
                viewModel.updateEmployee(finalUser)
            }
            navigator.pop()
        }

        Scaffold(
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(if (userToEdit == null) "Add New User" else "Edit User Profile") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { saveUser() },
                            enabled = fullName.isNotBlank() && username.isNotBlank() && (userToEdit != null || password.isNotBlank())
                        ) {
                            Text("SAVE", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                EditSection(title = "Account Information") {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Person, null) },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = userToEdit == null,
                        leadingIcon = { Icon(Icons.Default.AccountCircle, null) },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (userToEdit == null) "Password" else "Change Password") },
                        placeholder = { if (userToEdit != null) Text("Leave blank to keep current") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                }

                EditSection(title = "Contact & Work Details") {
                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = { mobileNumber = it },
                        label = { Text("Mobile Number") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = employeeCode,
                        onValueChange = { employeeCode = it },
                        label = { Text("Employee Code") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Info, null) },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }

                EditSection(title = "Role & Permissions") {
                    Text("Assigned Role", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserRole.entries.filter { it != UserRole.BENEFICIARY }.forEach { r ->
                            FilterChip(
                                selected = role == r,
                                onClick = { role = r },
                                label = { Text(r.name) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text("Account Status", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserStatus.entries.forEach { s ->
                            FilterChip(
                                selected = status == s,
                                onClick = { status = s },
                                label = { Text(s.name) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when(s) {
                                        UserStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                                        UserStatus.SUSPENDED -> MaterialTheme.colorScheme.errorContainer
                                        UserStatus.DISABLED -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                
                Button(
                    onClick = { saveUser() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = fullName.isNotBlank() && username.isNotBlank() && (userToEdit != null || password.isNotBlank())
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (userToEdit == null) "Create Account" else "Update Profile", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EditSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        content()
    }
}
