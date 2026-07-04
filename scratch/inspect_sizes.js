const fs = require('fs');
const path = require('path');

const assetsDir = "c:\\Users\\ravip\\AndroidStudioProjects\\PoliceMobileDirectory\\assets";
const files = fs.readdirSync(assetsDir);
files.forEach(f => {
    if (f.toLowerCase().endsWith('.png')) {
        const fullPath = path.join(assetsDir, f);
        const stats = fs.statSync(fullPath);
        console.log(`${f}: ${stats.size} bytes`);
    }
});
