const fs = require('fs');
const path = require('path');

const brainDir = "C:\\Users\\ravip\\.gemini\\antigravity\\brain\\cc2a4ac8-491f-4ef1-8e68-260091d4b65b";
console.log(`Checking files in: ${brainDir}`);

function walk(dir) {
    let results = [];
    let list;
    try {
        list = fs.readdirSync(dir);
    } catch (e) {
        return results;
    }
    list.forEach(file => {
        const fullPath = path.join(dir, file);
        let stat;
        try {
            stat = fs.statSync(fullPath);
        } catch (e) {
            return;
        }
        if (stat && stat.isDirectory()) {
            results = results.concat(walk(fullPath));
        } else {
            results.push({ path: fullPath, size: stat.size, mtime: stat.mtime });
        }
    });
    return results;
}

if (fs.existsSync(brainDir)) {
    const files = walk(brainDir);
    console.log(`Found ${files.length} total files:`);
    files.forEach(f => {
        const relative = path.relative(brainDir, f.path);
        console.log(`- ${relative}: ${f.size} bytes (Modified: ${f.mtime.toISOString()})`);
    });
} else {
    console.log("Brain directory does not exist!");
}
