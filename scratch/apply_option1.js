const fs = require('fs');
const filePath = 'app/src/user/java/com/example/policemobiledirectory/ui/screens/LoginScreen.kt';
let content = fs.readFileSync(filePath, 'utf8');

// 1. Add missing imports
const imports = [
    'import androidx.compose.ui.graphics.Brush',
    'import androidx.compose.foundation.shape.RoundedCornerShape',
    'import androidx.compose.ui.draw.shadow',
    'import androidx.compose.material.icons.filled.Lock'
];

imports.forEach(imp => {
    if (!content.includes(imp)) {
        content = content.replace('import androidx.compose.ui.graphics.Color', 'import androidx.compose.ui.graphics.Color\n' + imp);
    }
});

// 2. Define the new Option 1 UI Structure
const newUI = `
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- Top Gradient Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF00796B),
                            Color(0xFF009688)
                        )
                    )
                )
        )

        // --- Main Content ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // Logo in the header
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(110.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(4.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(24.dp))

            // White Card for the form
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(20.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Police Mobile Directory",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00796B),
                            fontSize = 24.sp
                        )
                    )
                    
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Exclusively for Karnataka State Police Department personnel.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Spacer(Modifier.height(32.dp))

                    // Google Login Button
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGoogleSignInClicked()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009688))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Divider(Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                        Text("  or  ", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                        Divider(Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                    }

                    Spacer(Modifier.height(24.dp))

                    // Offline Login Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEmailPinExpanded = !isEmailPinExpanded },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9ECEF))
                    ) {
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
                        }
                    }
                }
            }
            
            Spacer(Modifier.weight(1f))

            // Footer
            Text(
                text = "Developed By Ravikumar J, AHC, DAR\\nChikkaballapura",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = Color.Gray.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }`;

const uiStartTag = '// --- UI ---';
const uiStartIndex = content.indexOf(uiStartTag);

if (uiStartIndex !== -1) {
    const header = content.substring(0, uiStartIndex + uiStartTag.length);
    fs.writeFileSync(filePath, header + '\n' + newUI + '\n}', 'utf8');
    console.log('LoginScreen.kt updated successfully.');
} else {
    console.log('ERROR: UI start tag not found.');
}
