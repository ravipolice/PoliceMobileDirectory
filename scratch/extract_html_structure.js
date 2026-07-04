const fs = require('fs');
const path = require('path');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
const html = fs.readFileSync(htmlPath, 'utf8');

// Replace base64 data to make it readable
const cleanHtml = html.replace(/src="data:image\/[^;]+;base64,[^"]+"/g, 'src="data:image/...[BASE64]..."');

// Output array
const output = [];
const lines = cleanHtml.split('\n');
lines.forEach((line, index) => {
    const trimmed = line.trim();
    if (!trimmed) return;
    if (trimmed.length > 500) {
        output.push(`${index + 1}: ${trimmed.substring(0, 500)}... [TRUNCATED]`);
    } else {
        output.push(`${index + 1}: ${trimmed}`);
    }
});

fs.writeFileSync(path.join(__dirname, 'structure_output.txt'), output.join('\n'), 'utf8');
console.log('Successfully wrote structure_output.txt');
