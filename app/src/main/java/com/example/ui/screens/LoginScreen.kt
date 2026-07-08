package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var isOwnerLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 2FA Step State
    var step2FA by remember { mutableStateOf(false) }
    var loginToken2FA by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-focus the OTP input when entering 2FA and show keyboard
    LaunchedEffect(step2FA) {
        if (step2FA) {
            kotlinx.coroutines.delay(300)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val gradientBrush = Brush.linearGradient(
        colors = listOf(IndigoPrimary, Slate900)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                .testTag("login_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header Logo
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.ic_logo_foreground),
                        contentDescription = "Logo",
                        modifier = Modifier.size(100.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CrossMart",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = IndigoPrimary,
                        letterSpacing = 0.5.sp
                    )
                )

                Text(
                    text = "Storefront Management Console",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Slate400,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                AnimatedContent(
                    targetState = step2FA,
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    },
                    label = "LoginTransition"
                ) { targetStep ->
                    if (!targetStep) {
                        // --- Credentials Form Screen ---
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Account Type Switcher
                            TabRow(
                                selectedTabIndex = if (isOwnerLogin) 0 else 1,
                                containerColor = Color.Transparent,
                                contentColor = IndigoPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                            ) {
                                Tab(
                                    selected = isOwnerLogin,
                                    onClick = {
                                        isOwnerLogin = true
                                    },
                                    text = { Text("Owner Login", fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.testTag("owner_tab")
                                )
                                Tab(
                                    selected = !isOwnerLogin,
                                    onClick = {
                                        isOwnerLogin = false
                                    },
                                    text = { Text("Staff Login", fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.testTag("staff_tab")
                                )
                            }

                            // Username / Email input
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text(if (isOwnerLogin) "Email Address" else "Staff Username") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isOwnerLogin) Icons.Default.Email else Icons.Default.Person,
                                        contentDescription = "UserIcon",
                                        tint = IndigoPrimary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("username_input"),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = if (isOwnerLogin) KeyboardType.Email else KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Password Input
                            var passwordVisible by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "LockIcon",
                                        tint = IndigoPrimary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "TogglePassword"
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("password_input"),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                )
                            )

                            // Error Display
                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = RoseDanger,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Submit Button
                            Button(
                                onClick = {
                                    if (isOwnerLogin) {
                                        viewModel.loginOwner(email, password, onStep2FA = { token ->
                                            loginToken2FA = token
                                            step2FA = true
                                        }, onAutoLogin = onLoginSuccess)
                                    } else {
                                        viewModel.loginStaff(email, password, onStep2FA = { token ->
                                            loginToken2FA = token
                                            step2FA = true
                                        }, onAutoLogin = onLoginSuccess)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("login_submit_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Sign In Securely", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    color = IndigoPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { /* Simulate reset flow */ }
                                )
                                Text(
                                    text = "Create Shop",
                                    color = IndigoPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { /* Webpanel signup instructions */ }
                                )
                            }
                        }
                    } else {
                        // --- 2FA OTP Passcode Screen ---
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Two-Factor Verification",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Slate800,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "We sent a 6-digit verification code to your Telegram Bot. Enter it below to authorize this session.",
                                color = Slate400,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                            )

                            // OTP Boxes Layout
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        focusRequester.requestFocus()
                                        keyboardController?.show()
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                (0 until 6).forEach { index ->
                                    val char = verificationCode.getOrNull(index)?.toString() ?: ""
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Slate100)
                                            .border(
                                                width = if (verificationCode.length == index) 2.dp else 1.dp,
                                                color = if (verificationCode.length == index) IndigoPrimary else Slate200,
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = char,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = Slate800
                                        )
                                    }
                                }
                            }

                            // Invisible keyboard driver input
                            OutlinedTextField(
                                value = verificationCode,
                                onValueChange = { input ->
                                    if (input.length <= 6 && input.all { it.isDigit() }) {
                                        verificationCode = input
                                        if (input.length == 6) {
                                            keyboardController?.hide()
                                            // Submit verify
                                            if (isOwnerLogin) {
                                                viewModel.verifyOwnerLogin(loginToken2FA, input) {
                                                    onLoginSuccess()
                                                }
                                            } else {
                                                viewModel.verifyStaffLogin(loginToken2FA, input) {
                                                    onLoginSuccess()
                                                }
                                            }
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .size(1.dp)
                                    .focusRequester(focusRequester)
                                    .testTag("otp_hidden_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Polling Spinner Row
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = IndigoPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Waiting for Telegram approval...",
                                    color = Slate400,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage ?: "",
                                    color = RoseDanger,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Submit verification manually if auto-verify keyboard fails
                            Button(
                                onClick = {
                                    if (verificationCode.length == 6) {
                                        if (isOwnerLogin) {
                                            viewModel.verifyOwnerLogin(loginToken2FA, verificationCode) {
                                                onLoginSuccess()
                                            }
                                        } else {
                                            viewModel.verifyStaffLogin(loginToken2FA, verificationCode) {
                                                onLoginSuccess()
                                            }
                                        }
                                    }
                                },
                                enabled = verificationCode.length == 6 && !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("otp_verify_button")
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text("Verify & Authorize", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Back to Credentials",
                                color = IndigoPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        step2FA = false
                                        verificationCode = ""
                                        viewModel.stopPollingLogin()
                                    }
                                    .testTag("back_to_credentials")
                            )
                        }
                    }
                }
            }
        }
    }
}
