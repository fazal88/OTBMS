package com.olivetrust.charity.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
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

        LaunchedEffect(state) {
            if (state is LoginState.Success) {
                navigator.replaceAll(DashboardScreen())
            }
        }

        LoginContent(
            state = state,
            onLogin = { username, password -> viewModel.login(username, password) }
        )
    }
}

@Composable
fun LoginContent(
    state: LoginState,
    onLogin: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loginType by remember { mutableStateOf("User") } // "User" or "Beneficiary"
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Olive Trust", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        /*SingleChoiceSegmentedButtonRow(
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
        }*/

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(if (loginType == "User") "Username" else "Beneficiary ID") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "Hide" else "Show",
                        fontSize = 12.sp
                    )

                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onLogin(username, password)
                }
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (state is LoginState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onLogin(username, password)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }

        if (state is LoginState.Error) {
            Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Preview
@Composable
fun LoginContentPreview() {
    MaterialTheme {
        LoginContent(
            state = LoginState.Idle,
            onLogin = { _, _ -> }
        )
    }
}

@Preview
@Composable
fun LoginContentErrorPreview() {
    MaterialTheme {
        LoginContent(
            state = LoginState.Error("Invalid credentials"),
            onLogin = { _, _ -> }
        )
    }
}

@Preview
@Composable
fun LoginContentLoadingPreview() {
    MaterialTheme {
        LoginContent(
            state = LoginState.Loading,
            onLogin = { _, _ -> }
        )
    }
}
