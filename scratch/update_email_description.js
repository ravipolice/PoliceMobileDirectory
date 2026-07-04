const fs = require('fs');
const path = require('path');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
if (!fs.existsSync(htmlPath)) {
    console.error('HTML file not found!');
    process.exit(1);
}

let content = fs.readFileSync(htmlPath, 'utf8');

const target = 'Automatically filled from Google sign-in and disabled.';
const replacement = 'Automatically filled from Google sign-in and edit disabled.';

if (content.includes(target)) {
    content = content.replace(target, replacement);
    console.log('Successfully updated Email description in the HTML table.');
    fs.writeFileSync(htmlPath, content, 'utf8');
} else {
    console.error('Target Email description text not found in HTML!');
}
