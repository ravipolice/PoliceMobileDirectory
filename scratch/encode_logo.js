const fs = require('fs');
const path = require('path');

const logoPath = path.join(__dirname, '..', 'public', 'logo.png');
if (fs.existsSync(logoPath)) {
    const base64Data = fs.readFileSync(logoPath).toString('base64');
    console.log('Success! Logo base64 length:', base64Data.length);
    console.log('Prefix:', base64Data.substring(0, 100));
} else {
    console.error('Logo file not found at:', logoPath);
}
