const fs = require('fs');
const filePath = 'app/src/user/java/com/example/policemobiledirectory/ui/screens/LoginScreen.kt';
let content = fs.readFileSync(filePath, 'utf8');

const target = '        // Foreground content with padding';
const overlay = [
    '        // Dark overlay: keeps text/buttons readable over bright neon background',
    '        Box(',
    '            modifier = Modifier',
    '                .fillMaxSize()',
    '                .background(androidx.compose.ui.graphics.Color(0x8C000000))',
    '        )',
    '',
    '        // Foreground content with padding'
].join('\n');

if (content.includes(target)) {
    content = content.replace(target, overlay);
    fs.writeFileSync(filePath, content, 'utf8');
    console.log('Done! Dark overlay added successfully.');
} else {
    console.log('ERROR: Target string not found. Check the file manually.');
}
