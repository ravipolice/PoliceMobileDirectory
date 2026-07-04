const fs = require('fs');
const path = require('path');

const tempMediaDir = "C:\\Users\\ravip\\.gemini\\antigravity\\brain\\tempmediaStorage";
console.log(`Checking temp media directory: ${tempMediaDir}`);

if (!fs.existsSync(tempMediaDir)) {
    console.log("Directory does not exist!");
    process.exit(0);
}

try {
    const files = fs.readdirSync(tempMediaDir);
    console.log(`Found ${files.length} files:`);
    files.forEach(f => {
        const fullPath = path.join(tempMediaDir, f);
        const stats = fs.statSync(fullPath);
        console.log(`- ${f}: ${stats.size} bytes`);
    });
} catch (e) {
    console.log(`Error: ${e.message}`);
}
