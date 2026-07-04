const fs = require('fs');
const path = require('path');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
if (!fs.existsSync(htmlPath)) {
    console.error('HTML file not found!');
    process.exit(1);
}

let content = fs.readFileSync(htmlPath, 'utf8');

const target = '<strong>Mobile Number:</strong> The mobile number used during registration will publish in app and will be used for communication.';
const replacement = '<strong>Mobile Number:</strong> The mobile number used during registration will publish in app and will be used for communication.[CUG mobile number Disabled for Registration]';

if (content.includes(target)) {
    content = content.replace(target, replacement);
    console.log('Successfully updated Mobile Number prerequisite copy in HTML.');
    fs.writeFileSync(htmlPath, content, 'utf8');
} else {
    console.error('Target Mobile Number prerequisite copy not found in HTML!');
}
