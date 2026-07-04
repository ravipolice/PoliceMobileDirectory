const fs = require('fs');
const path = require('path');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
const content = fs.readFileSync(htmlPath, 'utf8');
const lines = content.split('\n');

function printClean(start, end) {
    for (let i = start - 1; i < end; i++) {
        let text = lines[i];
        if (!text) continue;
        if (text.includes('data:image/')) {
            text = text.replace(/(data:image\/[^;]+;base64,)[A-Za-z0-9+/=]{100,}/g, '$1[BASE64_TRUNCATED...]');
        }
        console.log(`Line ${i + 1}: ${text}`);
    }
}

console.log('--- STEP 1 AREA ---');
printClean(415, 435);

console.log('--- PAGE 2 DOWNLOAD AREA ---');
printClean(585, 608);
