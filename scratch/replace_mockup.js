const fs = require('fs');
const path = require('path');

const croppedImagePath = path.join(__dirname, 'mockup_cropped.jpg');
const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');

if (!fs.existsSync(croppedImagePath)) {
    console.error('Cropped image not found! Please run crop_image.js first.');
    process.exit(1);
}
if (!fs.existsSync(htmlPath)) {
    console.error('HTML file not found!');
    process.exit(1);
}

const croppedBase64 = fs.readFileSync(croppedImagePath).toString('base64');
let htmlContent = fs.readFileSync(htmlPath, 'utf8');

// Find the line containing <img class="mockup-img" and replace it
const lines = htmlContent.split('\n');
let replaced = false;

for (let i = 0; i < lines.length; i++) {
    if (lines[i].includes('class="mockup-img"') && lines[i].includes('src="data:image/jpeg;base64,')) {
        lines[i] = `                    <img class="mockup-img" style="width: 85%; max-height: 135mm; margin-bottom: 8px; object-fit: contain;" src="data:image/jpeg;base64,${croppedBase64}" alt="PMD Login Interface">`;
        replaced = true;
        console.log(`Replaced mockup image on line ${i + 1}.`);
        break;
    }
}

if (replaced) {
    htmlContent = lines.join('\n');
    fs.writeFileSync(htmlPath, htmlContent, 'utf8');
    console.log('Successfully updated HTML guide with cropped and resized mockup image.');
} else {
    console.error('Could not find mockup image tag in HTML!');
}
