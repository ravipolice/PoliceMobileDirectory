const fs = require('fs');
const filePath = 'app/src/user/java/com/example/policemobiledirectory/ui/screens/LoginScreen.kt';
let content = fs.readFileSync(filePath, 'utf8');

// 1. Update Gradient to be deeper and more vibrant
const newGradient = `                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF004D40), // Deep Midnight Teal
                            Color(0xFF00796B), // Primary Teal Dark
                            Color(0xFF009688)  // Primary Teal
                        )
                    )`;
content = content.replace(/Brush\.verticalGradient\([\s\S]+?\)/, newGradient);

// 2. Increase Logo Size and add thicker border
content = content.replace('.size(110.dp)', '.size(125.dp)');
content = content.replace('.padding(4.dp)', '.padding(6.dp)');

// 3. Polish the Google Button (White with Teal border for a premium look)
const polishedGoogleButton = `                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onGoogleSignInClicked()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .border(1.5.dp, Color(0xFF009688), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF37474F)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text("Sign in with Google", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }`;

// Find and replace the Google button block
const buttonStartTag = '// Google Login Button';
const buttonEndTag = '                    Spacer(Modifier.height(24.dp))';
const btnStartIndex = content.indexOf(buttonStartTag);
const btnEndIndex = content.indexOf(buttonEndTag, btnStartIndex);

if (btnStartIndex !== -1 && btnEndIndex !== -1) {
    content = content.substring(0, btnStartIndex + buttonStartTag.length) + '\n' + polishedGoogleButton + '\n' + content.substring(btnEndIndex);
}

// 4. Polish the Title
content = content.replace('fontSize = 24.sp', 'fontSize = 26.sp, letterSpacing = 0.5.sp');

// 5. Polish the Footer
const polishedFooter = `            Text(
                text = "Developed By Ravikumar J, AHC, DAR\\nChikkaballapura",
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp,
                    letterSpacing = 0.8.sp
                ),
                textAlign = TextAlign.Center,
                color = Color.DarkGray.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 32.dp)
            )`;
const footerStartTag = '// Footer';
const footerStartIndex = content.indexOf(footerStartTag);
const footerEndIndex = content.lastIndexOf(')'); // End of footer Text

if (footerStartIndex !== -1) {
    content = content.substring(0, footerStartIndex + footerStartTag.length) + '\n' + polishedFooter + content.substring(content.indexOf('        }', footerStartIndex));
}

fs.writeFileSync(filePath, content, 'utf8');
console.log('Premium Polish applied successfully.');
