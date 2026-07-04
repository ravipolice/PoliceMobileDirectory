const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
const pdfPath = path.join(__dirname, '..', 'user_registration_guide.pdf');

// Common installation paths for Google Chrome on Windows
const paths = [
    'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
    'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe',
    path.join(process.env.LOCALAPPDATA || '', 'Google', 'Chrome', 'Application', 'chrome.exe')
];

let chromePath = null;
for (const p of paths) {
    if (fs.existsSync(p)) {
        chromePath = p;
        break;
    }
}

if (!chromePath) {
    console.error('Google Chrome executable not found in common Windows paths!');
    console.log('Paths checked:');
    paths.forEach(p => console.log(' - ' + p));
    process.exit(1);
}

console.log('Found Chrome at:', chromePath);
console.log('Generating PDF from HTML...');

// Run headless Chrome to convert HTML to PDF
// --headless=new is the modern headless mode
// --print-to-pdf-no-header disables headers and footers (which we already have custom styled in HTML)
const cmd = `"${chromePath}" --headless=new --disable-gpu --print-to-pdf-no-header --print-to-pdf="${pdfPath}" "${htmlPath}"`;

exec(cmd, (error, stdout, stderr) => {
    if (error) {
        console.error('Error compiling PDF:', error);
        console.error(stderr);
        process.exit(1);
    }
    
    if (fs.existsSync(pdfPath)) {
        console.log('Success! PDF generated at:', pdfPath);
        const stats = fs.statSync(pdfPath);
        console.log('File size:', (stats.size / 1024).toFixed(2), 'KB');
    } else {
        console.error('Chrome completed but PDF file was not created!');
    }
});
