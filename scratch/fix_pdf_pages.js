const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
const pdfPath = path.join(__dirname, '..', 'user_registration_guide.pdf');

if (!fs.existsSync(htmlPath)) {
    console.error('HTML file not found!');
    process.exit(1);
}

let content = fs.readFileSync(htmlPath, 'utf8');

// Target position to insert @page style: right before @media print
const target = '@media print {';
const replacement = `@page {
            size: A4;
            margin: 0;
        }
        @media print {`;

if (content.includes(target)) {
    if (!content.includes('size: A4;')) {
        content = content.replace(target, replacement);
        console.log('Successfully inserted @page CSS rule.');
        fs.writeFileSync(htmlPath, content, 'utf8');
    } else {
        console.log('@page CSS rule already present.');
    }
} else {
    console.error('Could not find "@media print {" block in HTML!');
    process.exit(1);
}

// Find Chrome
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
    console.error('Google Chrome not found!');
    process.exit(1);
}

// Regenerate PDF
const cmd = `"${chromePath}" --headless=new --disable-gpu --print-to-pdf-no-header --print-to-pdf="${pdfPath}" "${htmlPath}"`;

console.log('Regenerating PDF...');
exec(cmd, (error, stdout, stderr) => {
    if (error) {
        console.error('Error generating PDF:', error);
        process.exit(1);
    }
    
    if (fs.existsSync(pdfPath)) {
        console.log('PDF regenerated successfully.');
        const pdfContent = fs.readFileSync(pdfPath);
        
        // Find page count in PDF structure (PDF catalog page count)
        const pdfString = pdfContent.toString('binary');
        const match = pdfString.match(/\/Count\s+(\d+)/);
        if (match) {
            console.log('PDF Page Count:', match[1]);
        } else {
            console.log('Could not determine page count from binary data, but PDF file generated.');
        }
        console.log('New File size:', (pdfContent.length / 1024).toFixed(2), 'KB');
    } else {
        console.error('PDF file was not created.');
    }
});
