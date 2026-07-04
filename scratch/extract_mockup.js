const fs = require('fs');
const path = require('path');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
const content = fs.readFileSync(htmlPath, 'utf8');

// The mockup image is on line 588 or nearby, let's find the base64 string
const match = content.match(/src="data:image\/jpeg;base64,([^"]+)"/);
if (match) {
    const base64Data = match[1];
    const buffer = Buffer.from(base64Data, 'base64');
    const outputPath = path.join(__dirname, '..', 'scratch', 'mockup.jpg');
    fs.writeFileSync(outputPath, buffer);
    console.log('Successfully extracted mockup image to:', outputPath);
    console.log('Size of file:', buffer.length, 'bytes');
} else {
    console.error('Mockup base64 image not found in HTML!');
}
