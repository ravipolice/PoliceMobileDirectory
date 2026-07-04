const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const htmlPath = path.join(__dirname, '..', 'user_registration_guide.html');
const pdfPath = path.join(__dirname, '..', 'user_registration_guide_test.pdf');
const backupPath = htmlPath + '.tmp_cfg_bak';

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

function backup() {
    fs.copyFileSync(htmlPath, backupPath);
}

function restore() {
    if (fs.existsSync(backupPath)) {
        fs.copyFileSync(backupPath, htmlPath);
        fs.unlinkSync(backupPath);
    }
}

function runPdf() {
    try {
        if (fs.existsSync(pdfPath)) fs.unlinkSync(pdfPath);
        const cmd = `"${chromePath}" --headless=new --disable-gpu --print-to-pdf-no-header --print-to-pdf="${pdfPath}" "${htmlPath}"`;
        execSync(cmd, { stdio: 'ignore' });
        if (!fs.existsSync(pdfPath)) return 0;
        const pdfContent = fs.readFileSync(pdfPath);
        const pdfString = pdfContent.toString('binary');
        const matches = pdfString.match(/\/Type\s*\/Page\b/g);
        return matches ? matches.length : 0;
    } catch (e) {
        console.error('Error running PDF generation:', e.message);
        return -1;
    }
}

const configs = [
    {
        name: "Test 1: display: block on .page in print",
        fn: (content) => {
            // Replace the print media query for .page
            return content.replace(
                /\.page\s*\{\s*margin:\s*0;\s*box-shadow:\s*none;\s*page-break-after:\s*always;\s*width:\s*210mm;\s*height:\s*280mm;\s*\}/g,
                `.page {
                    display: block !important;
                    margin: 0 !important;
                    box-shadow: none !important;
                    page-break-after: always !important;
                    break-after: page !important;
                    width: 210mm !important;
                    height: 297mm !important;
                    min-height: 297mm !important;
                    max-height: 297mm !important;
                }`
            );
        }
    },
    {
        name: "Test 2: Test 1 + min-height: auto on .page overall",
        fn: (content) => {
            let res = content.replace(
                /\.page\s*\{\s*margin:\s*0;\s*box-shadow:\s*none;\s*page-break-after:\s*always;\s*width:\s*210mm;\s*height:\s*280mm;\s*\}/g,
                `.page {
                    display: block !important;
                    margin: 0 !important;
                    box-shadow: none !important;
                    page-break-after: always !important;
                    break-after: page !important;
                    width: 210mm !important;
                    height: 297mm !important;
                    min-height: 297mm !important;
                    max-height: 297mm !important;
                }`
            );
            res = res.replace("min-height: 297mm;", "min-height: auto;");
            return res;
        }
    },
    {
        name: "Test 3: Test 2 + page-break-inside: avoid on cards",
        fn: (content) => {
            let res = content.replace(
                /\.page\s*\{\s*margin:\s*0;\s*box-shadow:\s*none;\s*page-break-after:\s*always;\s*width:\s*210mm;\s*height:\s*280mm;\s*\}/g,
                `.page {
                    display: block !important;
                    margin: 0 !important;
                    box-shadow: none !important;
                    page-break-after: always !important;
                    break-after: page !important;
                    width: 210mm !important;
                    height: 297mm !important;
                    min-height: 297mm !important;
                    max-height: 297mm !important;
                }`
            );
            res = res.replace("min-height: 297mm;", "min-height: auto;");
            res = res.replace(".section-card {", ".section-card { page-break-inside: avoid; break-inside: avoid;");
            return res;
        }
    },
    {
        name: "Test 4: Test 3 + slightly reduced print height to 296mm to prevent subpixel rounding breaks",
        fn: (content) => {
            let res = content.replace(
                /\.page\s*\{\s*margin:\s*0;\s*box-shadow:\s*none;\s*page-break-after:\s*always;\s*width:\s*210mm;\s*height:\s*280mm;\s*\}/g,
                `.page {
                    display: block !important;
                    margin: 0 !important;
                    box-shadow: none !important;
                    page-break-after: always !important;
                    break-after: page !important;
                    width: 210mm !important;
                    height: 296mm !important;
                    min-height: 296mm !important;
                    max-height: 296mm !important;
                }`
            );
            res = res.replace("min-height: 297mm;", "min-height: auto;");
            res = res.replace(".section-card {", ".section-card { page-break-inside: avoid; break-inside: avoid;");
            return res;
        }
    },
    {
        name: "Test 5: Set height of .page to 297mm, min-height to 297mm, overflow: hidden, and ensure block display",
        fn: (content) => {
            // Let's replace the whole style for .page and @media print to be super clean
            let res = content.replace(
                /min-height: 297mm;[\s\S]*?justify-content: flex-start;\s*\}/,
                `min-height: 297mm;\n    height: 297mm;\n    padding: 10mm 12mm;\n    margin: 20px auto;\n    background: white;\n    box-shadow: 0 10px 25px rgba(0,0,0,0.3);\n    position: relative;\n    overflow: hidden;\n    display: flex;\n    flex-direction: column;\n    justify-content: flex-start;\n    box-sizing: border-box;\n}`
            );
            res = res.replace(
                /\.page\s*\{\s*margin:\s*0;\s*box-shadow:\s*none;\s*page-break-after:\s*always;\s*width:\s*210mm;\s*height:\s*280mm;\s*\}/g,
                `.page {\n        margin: 0 !important;\n        box-shadow: none !important;\n        page-break-after: always !important;\n        break-after: page !important;\n        width: 210mm !important;\n        height: 297mm !important;\n        min-height: 297mm !important;\n        max-height: 297mm !important;\n        display: block !important;\n        overflow: hidden !important;\n    }`
            );
            return res;
        }
    }
];

backup();
try {
    const originalContent = fs.readFileSync(htmlPath, 'utf8');
    for (const cfg of configs) {
        console.log(`Running: ${cfg.name}`);
        const modified = cfg.fn(originalContent);
        fs.writeFileSync(htmlPath, modified, 'utf8');
        const count = runPdf();
        console.log(` -> Page count: ${count}`);
        if (count === 2) {
            console.log(`!!! SUCCESS WITH CONFIG: ${cfg.name} !!!`);
        }
    }
} catch (e) {
    console.error('Global Error:', e);
} finally {
    restore();
    console.log('Restored original HTML.');
    if (fs.existsSync(pdfPath)) {
        try { fs.unlinkSync(pdfPath); } catch(_) {}
    }
}
