const fs = require('fs');
const path = require('path');

const geminiDir = "C:\\Users\\ravip\\.gemini";
console.log(`Searching all files in: ${geminiDir}`);

const tenMinutesAgo = Date.now() - 10 * 60 * 1000;

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
                if (stat.mtimeMs >= tenMinutesAgo) {
                    results.push({ path: fullPath, size: stats = stat.size, mtime: stat.mtime });
                }
            }
        }
    });
    return results;
}

if (fs.existsSync(geminiDir)) {
    const files = walk(geminiDir);
    console.log(`Found ${files.length} recently modified images:`);
    files.forEach(f => {
        console.log(`- ${f.path} (${f.size} bytes) - Modified: ${f.mtime.toISOString()}`);
    });
} else {
    console.log("Gemini directory does not exist!");
}
