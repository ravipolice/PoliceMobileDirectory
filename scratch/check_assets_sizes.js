const fs = require('fs');
const path = require('path');

const assetsDir = "c:\\Users\\ravip\\AndroidStudioProjects\\PoliceMobileDirectory\\assets";
console.log(`Checking assets sizes in: ${assetsDir}`);

if (!fs.existsSync(assetsDir)) {
    console.log("Assets directory does not exist!");
    process.exit(0);
}

try {
    const files = fs.readdirSync(assetsDir);
    console.log(`Found ${files.length} files:`);
    files.forEach(f => {
        const fullPath = path.join(assetsDir, f);
        const stats = fs.statSync(fullPath);
        console.log(`- ${f}: ${stats.size} bytes`);
    });
} catch (e) {
    console.log(`Error: ${e.message}`);
}
