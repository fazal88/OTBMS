package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

class LoginScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<LoginViewModel>()
        val state by viewModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        var loginType by remember { mutableStateOf("User") } // "User" or "Beneficiary"

        LaunchedEffect(state) {
            if (state is LoginState.Success) {
                navigator.replaceAll(DashboardScreen())
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Olive Trust", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(32.dp))

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                SegmentedButton(
                    selected = loginType == "User",
                    onClick = { loginType = "User" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("User")
                }
                SegmentedButton(
                    selected = loginType == "Beneficiary",
                    onClick = { loginType = "Beneficiary" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Beneficiary")
                }
            }
            
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(if (loginType == "User") "Username" else "Beneficiary ID") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "Hide" else "Show")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (state is LoginState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.login(username, password) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Login")
                }
            }
            
            if (state is LoginState.Error) {
                Text(
                    text = (state as LoginState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
