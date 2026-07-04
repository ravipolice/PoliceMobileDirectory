const fs = require('fs');
const path = require('path');
const { exec } = require('child_process');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
const pdfPath = path.join(__dirname, '..', 'user_registration_guide.pdf');

// Save a backup of the HTML file
const backupPath = htmlPath + '.bak';
fs.copyFileSync(htmlPath, backupPath);
console.log('Created backup of HTML at:', backupPath);

// Find Chrome path
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

async function runPdfGeneration() {
    const cmd = `"${chromePath}" --headless=new --disable-gpu --print-to-pdf-no-header --print-to-pdf="${pdfPath}" "${htmlPath}"`;
    await new Promise((resolve, reject) => {
        exec(cmd, (err) => {
            if (err) reject(err);
            else resolve();
        });
    });
    const pdfContent = fs.readFileSync(pdfPath);
    const pdfString = pdfContent.toString('binary');
    const matches = pdfString.match(/\/Type\s*\/Page\b/g);
    return matches ? matches.length : 0;
}

async function main() {
    try {
        let content = fs.readFileSync(htmlPath, 'utf8');
        
        // Let's modify the styles in content to make everything super compact:
        // 1. Reduce font-size on body
        content = content.replace("line-height: 1.5;", "line-height: 1.2;");
        // 2. Reduce padding on page
        content = content.replace("padding: 8mm 12mm 8mm 12mm;", "padding: 4mm 6mm;");
        content = content.replace("padding: 10mm 12mm;", "padding: 4mm 6mm;");
        // 3. Reduce card margins
        content = content.replace(/margin-bottom:\s*\d+px;/g, "margin-bottom: 2px;");
        content = content.replace(/margin-bottom:\s*\d+mm;/g, "margin-bottom: 2mm;");
        // 4. Reduce card paddings
        content = content.replace("padding: 8px 10px;", "padding: 3px 5px;");
        content = content.replace("padding: 10px 12px;", "padding: 3px 5px;");
        content = content.replace("padding: 8px 12px;", "padding: 3px 5px;");
        // 5. Reduce grid gaps
        content = content.replace("gap: 12px;", "gap: 4px;");
        content = content.replace("gap: 10px;", "gap: 4px;");
        content = content.replace("gap: 8px 15px;", "gap: 2px 4px;");
        // 6. Reduce mockup heights
        content = content.replace("max-height: 135mm;", "max-height: 70mm;");
        content = content.replace("max-height: 95mm;", "max-height: 50mm;");

        fs.writeFileSync(htmlPath, content, 'utf8');
        console.log('Modified HTML file to use compact layout.');

        const pageCount = await runPdfGeneration();
        console.log('Page count with compact layout:', pageCount);
        
        if (pageCount === 2) {
            console.log('Diagnosis confirmed! The 4-page count is caused by card/content overflow splitting pages.');
        } else {
            console.log('Page count is still not 2. There might be a hard layout break or page size issue.');
        }

    } catch (e) {
        console.error('Error:', e);
    } finally {
        // Restore HTML file from backup
        if (fs.existsSync(backupPath)) {
            fs.copyFileSync(backupPath, htmlPath);
            fs.unlinkSync(backupPath);
            console.log('Restored HTML file from backup.');
        }
    }
}

main();
