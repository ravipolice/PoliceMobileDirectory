const fs = require('fs');
const path = require('path');

const assetsDir = "c:\\Users\\ravip\\AndroidStudioProjects\\PoliceMobileDirectory\\assets";
console.log(`Inspecting headers in: ${assetsDir}`);

const files = fs.readdirSync(assetsDir);
files.forEach((f, idx) => {
    if (f.toLowerCase().endsWith('.png')) {
        const fullPath = path.join(assetsDir, f);
        try {
            const fd = fs.openSync(fullPath, 'r');
            const buffer = Buffer.alloc(16);
            fs.readSync(fd, buffer, 0, 16, 0);
            fs.closeSync(fd);
            
            const hex = buffer.toString('hex').toUpperCase();
            console.log(`[${idx}] ${f}`);
            console.log(`    Hex: ${hex}`);
            // Check for WebP
            if (hex.startsWith("52494646") && hex.includes("57454250", 16)) { // Wait, 57454250 is WEBP
                console.log(`    Type: WebP`);
            } else if (hex.startsWith("89504E47")) {
                console.log(`    Type: PNG`);
            } else if (hex.startsWith("FFD8FF")) {
                console.log(`    Type: JPEG`);
            } else {
                console.log(`    Type: Unknown`);
            }
        } catch (e) {
            console.log(`[${idx}] ${f} - Error: ${e.message}`);
        }
    }
});
