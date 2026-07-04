const fs = require('fs');
const path = require('path');

const cleanPdfPath = path.join(__dirname, '..', 'user_registration_guide_clean.pdf');
const originalPdfPath = path.join(__dirname, '..', 'user_registration_guide.pdf');

try {
    if (fs.existsSync(originalPdfPath)) {
        console.log('Attempting to overwrite original PDF...');
        // Let's try to write to it
        fs.copyFileSync(cleanPdfPath, originalPdfPath);
        console.log('Successfully updated original PDF at:', originalPdfPath);
    } else {
        fs.copyFileSync(cleanPdfPath, originalPdfPath);
        console.log('Successfully created original PDF at:', originalPdfPath);
    }
} catch (e) {
    console.error('Could not overwrite original PDF due to a file lock. Error:', e.message);
    console.log('The user likely has user_registration_guide.pdf open in their PDF viewer/browser.');
    console.log('Please instruct the user to close their PDF viewer or browser tab showing the PDF, then we can write to it.');
}
