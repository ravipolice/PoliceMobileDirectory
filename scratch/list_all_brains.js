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
                results.push({ path: fullPath, size: stat.size });
            }
        }
    });
    return results;
}

if (fs.existsSync(brainDir)) {
    const files = walk(brainDir);
    console.log(`Found ${files.length} image/media files:`);
    files.forEach(f => {
        console.log(`- ${f.path} (${f.size} bytes)`);
    });
} else {
    console.log("Brain directory does not exist!");
}
