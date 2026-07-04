const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const pdfPath = path.join(__dirname, '..', 'user_registration_guide.pdf');
const testPdfPath = path.join(__dirname, '..', 'user_registration_guide_test.pdf');

// Delete files
if (fs.existsSync(pdfPath)) {
    try { fs.unlinkSync(pdfPath); console.log('Deleted user_registration_guide.pdf'); } catch(e) { console.error('Could not delete user_registration_guide.pdf:', e.message); }
}
if (fs.existsSync(testPdfPath)) {
    try { fs.unlinkSync(testPdfPath); console.log('Deleted user_registration_guide_test.pdf'); } catch(e) { console.error('Could not delete test pdf:', e.message); }
}

// Run the official generate_pdf.js script
console.log('Running node scratch/generate_pdf.js...');
try {
    execSync('node scratch/generate_pdf.js', { stdio: 'inherit' });
} catch(e) {
    console.error('Error running generate_pdf.js:', e.message);
}

// Check the page count of user_registration_guide.pdf
if (fs.existsSync(pdfPath)) {
    const pdfContent = fs.readFileSync(pdfPath);
    const pdfString = pdfContent.toString('binary');
    const matches = pdfString.match(/\/Type\s*\/Page\b/g);
    console.log('PDF page count matches (/Type /Page):', matches ? matches.length : 0);
    
    let countMatch;
    const regex = /\/Count\s+(\d+)/g;
    while ((countMatch = regex.exec(pdfString)) !== null) {
        console.log('Found page tree node count:', countMatch[1]);
    }
} else {
    console.error('user_registration_guide.pdf was not generated!');
}
