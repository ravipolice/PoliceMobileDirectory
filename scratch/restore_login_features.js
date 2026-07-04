const fs = require('fs');
const filePath = 'app/src/user/java/com/example/policemobiledirectory/ui/screens/LoginScreen.kt';
let content = fs.readFileSync(filePath, 'utf8');

// 1. Add loading overlay at the end of the Box
const loadingOverlay = `
        // --- Loading Overlay ---
        if (isLoading || isAccountPickerLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    if (isAccountPickerLoading) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Please wait...\\nLoading Google accounts",
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }`;

// 2. Add the actual PIN form fields inside the expansion block
const pinFormFields = `
                    // Offline Login Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEmailPinExpanded = !isEmailPinExpanded },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9ECEF))
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00796B))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "Login with Email and PIN",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF495057)
                                )
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    imageVector = if (isEmailPinExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }

                            if (isEmailPinExpanded) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("Email Address") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = pin,
                                        onValueChange = { if (it.length <= 4) pin = it },
                                        label = { Text("4-Digit PIN") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                        trailingIcon = {
                                            IconButton(onClick = { pinVisible = !pinVisible }) {
                                                Icon(
                                                    imageVector = if (pinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    )
                                    Spacer(Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            focusManager.clearFocus()
                                            viewModel.loginUser(email, pin)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        enabled = email.isNotBlank() && pin.length == 4,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
                                    ) {
                                        Text("Login", fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(
                                        onClick = onForgotPinClicked,
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Forgot PIN?", color = Color(0xFF00796B))
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }
                            }
                        }
                    }`;

// Replacement 1: Update the Offline Login Card
const oldOfflineCardStart = '                    // Offline Login Section';
const oldOfflineCardEnd = '                    }';
const startIndex = content.indexOf(oldOfflineCardStart);
const endIndex = content.indexOf(oldOfflineCardEnd, startIndex) + oldOfflineCardEnd.length;

if (startIndex !== -1) {
    content = content.substring(0, startIndex) + pinFormFields + content.substring(endIndex);
}

// Replacement 2: Append loading overlay before the last brace of the Box
const boxEndIndex = content.lastIndexOf('    }');
content = content.substring(0, boxEndIndex) + loadingOverlay + '\n' + content.substring(boxEndIndex);

// Add missing arrow icons
if (!content.includes('import androidx.compose.material.icons.filled.KeyboardArrowDown')) {
    content = content.replace(
        'import androidx.compose.material.icons.filled.Lock',
        'import androidx.compose.material.icons.filled.Lock\nimport androidx.compose.material.icons.filled.KeyboardArrowDown\nimport androidx.compose.material.icons.filled.KeyboardArrowUp\nimport androidx.compose.material.icons.filled.Visibility\nimport androidx.compose.material.icons.filled.VisibilityOff'
    );
}

fs.writeFileSync(filePath, content, 'utf8');
console.log('LoginScreen.kt fully restored with PIN fields and loading overlay.');
