const fs = require('fs');
const filePath = 'app/src/user/java/com/example/policemobiledirectory/ui/screens/LoginScreen.kt';
let content = fs.readFileSync(filePath, 'utf8');

// Define the correct block for the loading/login UI
const correctBlock = `        // Foreground content with padding
        if (isLoading || isAccountPickerLoading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                if (isAccountPickerLoading) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Please wait...\\nLoading Google accounts",
                        textAlign = TextAlign.Center,
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            // The main login form UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(16.dp))`;

// Find where the block starts and ends in the broken file
// It starts around line 279 (Foreground content with padding)
// It ends where the "Police Mobile Directory title" comment begins

const startTag = '        // Foreground content with padding';
const endTag = '                // Police Mobile Directory title';

const startIndex = content.indexOf(startTag);
const endIndex = content.indexOf(endTag);

if (startIndex !== -1 && endIndex !== -1) {
    const before = content.substring(0, startIndex);
    const after = content.substring(endIndex);
    const newContent = before + correctBlock + '\n' + after;
    
    // Also add missing imports for scrolling
    let finalContent = newContent;
    if (!finalContent.includes('import androidx.compose.foundation.rememberScrollState')) {
        finalContent = finalContent.replace(
            'import androidx.compose.foundation.background',
            'import androidx.compose.foundation.background\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll'
        );
    }

    fs.writeFileSync(filePath, finalContent, 'utf8');
    console.log('LoginScreen.kt structure restored and improved with systemBarsPadding.');
} else {
    console.log('Could not find start/end tags to fix the file.');
}
