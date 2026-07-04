const fs = require('fs');
const path = require('path');

const appData = process.env.APPDATA;
console.log(`APPDATA environment variable: ${appData}`);

if (!appData) {
    console.log("APPDATA env variable is not set!");
    process.exit(1);
}

const folders = fs.readdirSync(appData);
console.log("Directories in APPDATA:");
folders.forEach(f => {
    const fullPath = path.join(appData, f);
    try {
        const stat = fs.statSync(fullPath);
        if (stat.isDirectory()) {
            console.log(`- ${f}`);
        }
    } catch(e) {}
});
