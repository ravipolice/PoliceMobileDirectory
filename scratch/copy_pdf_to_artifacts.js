const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
const cleanPdfPath = path.join(__dirname, '..', 'user_registration_guide_clean.pdf');
const artifactDir = 'C:\\Users\\ravip\\.gemini\\antigravity\\brain\\397b39e0-6e04-41d1-af7c-dd51bacb94cb';
const artifactPdfPath = path.join(artifactDir, 'user_registration_guide_clean.pdf');

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
    console.error('Chrome not found');
    process.exit(1);
}

// Generate to cleanPdfPath
console.log('Generating PDF...');
if (fs.existsSync(cleanPdfPath)) {
    fs.unlinkSync(cleanPdfPath);
}
const cmd = `"${chromePath}" --headless=new --disable-gpu --print-to-pdf-no-header --print-to-pdf="${cleanPdfPath}" "${htmlPath}"`;
execSync(cmd, { stdio: 'inherit' });

if (fs.existsSync(cleanPdfPath)) {
    console.log('PDF generated successfully at:', cleanPdfPath);
    const pdfContent = fs.readFileSync(cleanPdfPath);
    const pdfString = pdfContent.toString('binary');
    const matches = pdfString.match(/\/Type\s*\/Page\b/g);
    console.log('Page count:', matches ? matches.length : 0);
    
    // Copy to artifact directory
    if (!fs.existsSync(artifactDir)) {
        fs.mkdirSync(artifactDir, { recursive: true });
    }
    fs.copyFileSync(cleanPdfPath, artifactPdfPath);
    console.log('Copied PDF to artifact directory:', artifactPdfPath);
} else {
    console.error('Failed to generate PDF');
}
