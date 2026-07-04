const fs = require('fs');
const path = require('path');

const cleanPdfPath = path.join(__dirname, '..', 'user_registration_guide_clean.pdf');
const targetPdfPath = path.join(__dirname, '..', 'Police_Mobile_Directory_Registration_Guide.pdf');

try {
    fs.copyFileSync(cleanPdfPath, targetPdfPath);
    console.log('Successfully copied to new path:', targetPdfPath);
} catch (e) {
    console.error('Error copying to new path:', e.message);
}
