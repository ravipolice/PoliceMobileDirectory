const fs = require('fs');
const path = require('path');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
const html = fs.readFileSync(htmlPath, 'utf8');

// Extract all <style>...</style> content
const styleRegex = /<style[^>]*>([\s\S]*?)<\/style>/gi;
let match;
let count = 1;
while ((match = styleRegex.exec(html)) !== null) {
    console.log(`--- Style Block ${count++} ---`);
    // Print lines that don't look like base64 or huge data to keep output readable
    const lines = match[1].split('\n');
    lines.forEach(line => {
        if (line.length < 300) {
            console.log(line);
        } else {
            console.log(line.substring(0, 100) + '... [TRUNCATED]');
        }
    });
}
