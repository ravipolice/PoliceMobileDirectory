const fs = require('fs');
const path = require('path');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
let content = fs.readFileSync(htmlPath, 'utf8');

const target1 = 'Install App from playstore using below provided link or QR code.';
const replacement1 = 'Install App from <a href="https://play.google.com/store/apps/details?id=com.pmd.userapp" target="_blank" style="color: var(--primary); font-weight: 600; text-decoration: underline;">playstore</a> using below provided link or QR code.';

if (content.includes(target1)) {
    content = content.replace(target1, replacement1);
    console.log('Successfully updated Step 1 with clickable playstore link.');
} else {
    console.error('Target 1 (Step 1 playstore text) not found in HTML!');
}

const target2 = '<p style="font-size: 10.5px; color: #475569; line-height: 1.4; margin: 0;">Scan the QR code above to download the official Android App (APK file) directly onto your mobile device, or contact your district administrator for assistance.</p>';
const replacement2 = '<p style="font-size: 10.5px; color: #475569; line-height: 1.4; margin: 0;">Scan the QR code above or <a href="https://play.google.com/store/apps/details?id=com.pmd.userapp" target="_blank" style="color: var(--primary); font-weight: 600; text-decoration: underline;">click here</a> to download the official Android App from Google Play Store directly onto your mobile device.</p>';

if (content.includes(target2)) {
    content = content.replace(target2, replacement2);
    console.log('Successfully updated Page 2 download card with clickable playstore link.');
} else {
    console.error('Target 2 (Page 2 download card text) not found in HTML!');
}

fs.writeFileSync(htmlPath, content, 'utf8');
console.log('Changes written to HTML file.');
