const fs = require('fs');
const path = require('path');

const targetDir = "C:\\Users\\ravip\\AppData\\Roaming\\Cursor\\User\\workspaceStorage\\ca06e50a6f0ba1b2f09ecce9df7ca871\\images";
console.log(`Checking Cursor images in: ${targetDir}`);

if (!fs.existsSync(targetDir)) {
    console.log("Directory does not exist!");
    process.exit(0);
}

try {
    const files = fs.readdirSync(targetDir);
    console.log(`Found ${files.length} files:`);
    files.forEach(f => {
        const fullPath = path.join(targetDir, f);
        const stats = fs.statSync(fullPath);
        console.log(`- ${f}: ${stats.size} bytes`);
    });
} catch (e) {
    console.log(`Error: ${e.message}`);
}
