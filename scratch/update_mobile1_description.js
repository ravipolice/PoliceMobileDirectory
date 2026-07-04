const fs = require('fs');
const path = require('path');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
if (!fs.existsSync(htmlPath)) {
    console.error('HTML file not found!');
    process.exit(1);
}

let content = fs.readFileSync(htmlPath, 'utf8');

const target = 'Enter active 10 to 13 digit phone number.';
const replacement = 'Enter active 10 digit phone number.';

if (content.includes(target)) {
    content = content.replace(target, replacement);
    console.log('Successfully updated Mobile 1 description in the HTML table.');
    fs.writeFileSync(htmlPath, content, 'utf8');
} else {
    console.error('Target Mobile 1 description text not found in HTML!');
}
