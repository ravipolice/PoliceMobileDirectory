const fs = require('fs');
const path = require('path');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
const content = fs.readFileSync(htmlPath, 'utf8');

// Find all matches for playstore, google play, qr code, href, etc.
const lines = content.split('\n');
lines.forEach((line, index) => {
    if (line.toLowerCase().includes('play.google.com') || 
        line.toLowerCase().includes('playstore') || 
        line.toLowerCase().includes('qr') || 
        line.toLowerCase().includes('download')) {
        console.log(`Line ${index + 1}: ${line.trim().substring(0, 150)}`);
    }
});
