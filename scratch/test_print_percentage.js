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

async function testWithStyle(replacementRule) {
    let content = fs.readFileSync(htmlPath, 'utf8');
    
    // Replace the @media print section
    const target = /@media print\s*\{[\s\S]*?\.page\s*\{[\s\S]*?width:\s*\w+;?\s*height:\s*\w+;?[\s\S]*?\}[\s\S]*?\}/;
    
    // Let's replace the whole @media print block with a clean new one
    const originalPrintBlockStart = content.indexOf('@media print {');
    if (originalPrintBlockStart === -1) {
        console.error('No @media print section found!');
        return;
    }
    const originalPrintBlockEnd = content.indexOf('}', content.indexOf('}', originalPrintBlockStart) + 1);
    
    const newPrintBlock = `@media print {
            body {
                background: none;
            }
            .page {
                margin: 0;
                box-shadow: none;
                page-break-after: always;
                width: 210mm;
                height: ${replacementRule};
            }
        }`;
        
    let updatedContent = content.substring(0, originalPrintBlockStart) + 
                         newPrintBlock + 
                         content.substring(originalPrintBlockEnd + 1);
                         
    fs.writeFileSync(htmlPath, updatedContent, 'utf8');
    
    // Compile PDF
    const cmd = `"${chromePath}" --headless=new --disable-gpu --print-to-pdf-no-header --print-to-pdf="${pdfPath}" "${htmlPath}"`;
    
    await new Promise((resolve) => {
        exec(cmd, () => resolve());
    });
    
    const pdfContent = fs.readFileSync(pdfPath);
    const pdfString = pdfContent.toString('binary');
    const matches = pdfString.match(/\/Type\s*\/Page\b/g);
    const pageCount = matches ? matches.length : 0;
    console.log(`With height: ${replacementRule} -> PDF Page Count: ${pageCount}`);
    return pageCount;
}

async function run() {
    try {
        // Test height: 100%;
        let count = await testWithStyle('100%');
        if (count === 2) {
            console.log('Success! height: 100% produced exactly 2 pages.');
            return;
        }

        // Test height: 100vh;
        count = await testWithStyle('100vh');
        if (count === 2) {
            console.log('Success! height: 100vh produced exactly 2 pages.');
            return;
        }

        // Test height: 270mm (which fits inside Letter height of 279mm)
        count = await testWithStyle('270mm');
        if (count === 2) {
            console.log('Success! height: 270mm produced exactly 2 pages.');
            return;
        }

        // Test height: 280mm
        count = await testWithStyle('280mm');
        if (count === 2) {
            console.log('Success! height: 280mm produced exactly 2 pages.');
            return;
        }

        console.log('Done testing.');
    } catch (e) {
        console.error(e);
    }
}

run();
