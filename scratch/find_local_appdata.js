const fs = require('fs');
const path = require('path');

const localAppData = process.env.LOCALAPPDATA || "C:\\Users\\ravip\\AppData\\Local";
console.log(`LOCALAPPDATA path: ${localAppData}`);

if (fs.existsSync(localAppData)) {
    const folders = fs.readdirSync(localAppData);
    console.log("Directories in LOCALAPPDATA:");
    folders.forEach(f => {
        const fullPath = path.join(localAppData, f);
        try {
            const stat = fs.statSync(fullPath);
            if (stat.isDirectory()) {
                console.log(`- ${f}`);
            }
        } catch(e) {}
    });
} else {
    console.log("LOCALAPPDATA does not exist!");
}
