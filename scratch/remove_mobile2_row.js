const fs = require('fs');
const path = require('path');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
if (!fs.existsSync(htmlPath)) {
    console.error('HTML file not found!');
    process.exit(1);
}

let content = fs.readFileSync(htmlPath, 'utf8');

// Target HTML block to remove
const target = `                                <tr>
                                    <td><strong>Mobile 2</strong></td>
                                    <td><span class="opt-badge">OPTIONAL</span></td>
                                    <td>Secondary mobile contact.</td>
                                </tr>`;

if (content.includes(target)) {
    content = content.replace(target, '');
    console.log('Successfully removed Mobile 2 row from the HTML table.');
    fs.writeFileSync(htmlPath, content, 'utf8');
} else {
    // Check if there are spacing differences
    const normalizedTarget = target.replace(/\s+/g, ' ').trim();
    const normalizedContent = content.replace(/\s+/g, ' ');
    if (normalizedContent.includes(normalizedTarget)) {
        console.log('Found Mobile 2 block with different formatting. Performing regex replacement...');
        // Let's do a regex replacement
        const regex = /<tr>\s*<td>\s*<strong>\s*Mobile 2\s*<\/strong>\s*<\/td>\s*<td>\s*<span class="opt-badge">\s*OPTIONAL\s*<\/span>\s*<\/td>\s*<td>\s*Secondary mobile contact\.\s*<\/td>\s*<\/tr>/i;
        content = content.replace(regex, '');
        fs.writeFileSync(htmlPath, content, 'utf8');
        console.log('Successfully removed Mobile 2 row via regex.');
    } else {
        console.error('Could not find Mobile 2 row in HTML!');
    }
}
