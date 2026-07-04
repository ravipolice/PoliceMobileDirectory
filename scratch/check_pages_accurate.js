const fs = require('fs');
const path = require('path');

const pdfPath = path.join(__dirname, '..', 'user_registration_guide.pdf');
if (!fs.existsSync(pdfPath)) {
    console.error('PDF file not found!');
    process.exit(1);
}

const pdfContent = fs.readFileSync(pdfPath);
const pdfString = pdfContent.toString('binary');

// Find all matches for /Type /Page (case-sensitive and handles whitespace)
const matches = pdfString.match(/\/Type\s*\/Page\b/g);
console.log('Number of "/Type /Page" occurrences:', matches ? matches.length : 0);

// Let's also check all /Count occurrences
let countMatch;
const regex = /\/Count\s+(\d+)/g;
while ((countMatch = regex.exec(pdfString)) !== null) {
    console.log('Found page tree node count:', countMatch[1]);
}
