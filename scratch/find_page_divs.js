const fs = require('fs');
const path = require('path');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
const content = fs.readFileSync(htmlPath, 'utf8');

const lines = content.split('\n');
lines.forEach((line, index) => {
    if (line.includes('class="page"')) {
        console.log(`Line ${index + 1}: ${line.trim()}`);
    }
});
