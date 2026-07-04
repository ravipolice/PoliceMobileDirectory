const fs = require('fs');
const path = require('path');

const startDir = "C:\\Users\\ravip\\AppData\\Roaming\\Cursor";
console.log(`Searching for cached images in: ${startDir}`);

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
                if (stat.size > 1000) { // Only files with contents (greater than 1KB)
                    results.push({ path: fullPath, size: stat.size });
                }
            }
        }
    });
    return results;
}

if (fs.existsSync(startDir)) {
    const images = walk(startDir);
    console.log(`Found ${images.length} cached images:`);
    images.forEach(img => {
        console.log(`- ${img.path} (${img.size} bytes)`);
    });
} else {
    console.log("Cursor data directory does not exist!");
}
