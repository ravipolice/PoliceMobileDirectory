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

async function testFix(heightValue) {
    let content = fs.readFileSync(htmlPath, 'utf8');
    
    // Find the @media print section and replace the height of .page
    // The original CSS is:
    // .page {
    //     margin: 0;
    //     box-shadow: none;
    //     page-break-after: always;
    //     width: 210mm;
    //     height: 297mm;
    // }
    
    const target = /height:\s*297mm;/g;
    // Note: there are two occurrences of height: 297mm; in the HTML (one in .page, one in @media print .page)
    // We want to replace the height of .page in @media print with heightValue.
    // Let's do a more precise replacement.
    
    // Let's find the "@media print" section first.
    const printSectionStart = content.indexOf('@media print');
    if (printSectionStart === -1) {
        console.error('No @media print section found!');
        return;
    }
    
    const heightIndex = content.indexOf('height: 297mm;', printSectionStart);
    if (heightIndex === -1) {
        console.error('No height: 297mm; found in @media print section!');
        return;
    }
    
    let updatedContent = content.substring(0, heightIndex) + 
                         `height: ${heightValue};` + 
                         content.substring(heightIndex + 'height: 297mm;'.length);
                         
    fs.writeFileSync(htmlPath, updatedContent, 'utf8');
    console.log(`Updated print height of .page to: ${heightValue}`);
    
    // Compile PDF
    const cmd = `"${chromePath}" --headless=new --disable-gpu --print-to-pdf-no-header --print-to-pdf="${pdfPath}" "${htmlPath}"`;
    
    await new Promise((resolve, reject) => {
        exec(cmd, (err, stdout, stderr) => {
            if (err) reject(err);
            else resolve();
        });
    });
    
    const pdfContent = fs.readFileSync(pdfPath);
    const pdfString = pdfContent.toString('binary');
    const matches = pdfString.match(/\/Type\s*\/Page\b/g);
    const pageCount = matches ? matches.length : 0;
    console.log(`For height: ${heightValue} -> PDF Page Count: ${pageCount}`);
    return pageCount;
}

async function run() {
    try {
        // Test 296mm
        let count = await testFix('296mm');
        if (count === 2) {
            console.log('Success! 296mm fixed the issue.');
            return;
        }
        
        // Test 295mm
        count = await testFix('295mm');
        if (count === 2) {
            console.log('Success! 295mm fixed the issue.');
            return;
        }
        
        // Test 100%
        count = await testFix('100%');
        if (count === 2) {
            console.log('Success! 100% fixed the issue.');
            return;
        }

        // Test auto
        count = await testFix('auto');
        if (count === 2) {
            console.log('Success! auto fixed the issue.');
            return;
        }
        
        console.log('None of the quick fixes resulted in exactly 2 pages. Let us inspect further.');
    } catch (e) {
        console.error(e);
    }
}

run();
