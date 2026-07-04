const fs = require('fs');
const path = require('path');

const brainDir = "C:\\Users\\ravip\\.gemini\\antigravity\\brain";
console.log(`Searching all brain directories in: ${brainDir}`);

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
            const ext = path.extname(file).toLowerCase();
            if (ext === '.png' || ext === '.jpg' || ext === '.jpeg') {
                // Ignore generated mockups, focus on media__ files or potential user screenshots
                results.push({ path: fullPath, size: stat.size, mtime: stat.mtime });
            }
        }
    });
    return results;
}

if (fs.existsSync(brainDir)) {
    let files = walk(brainDir);
    // Sort by modification time, newest first
    files.sort((a, b) => b.mtime - a.mtime);
    
    console.log("Most recent 30 images/media files:");
    files.slice(0, 30).forEach(f => {
        console.log(`- ${f.path} (${f.size} bytes) - Modified: ${f.mtime.toISOString()}`);
    });
} else {
    console.log("Brain directory does not exist!");
}
