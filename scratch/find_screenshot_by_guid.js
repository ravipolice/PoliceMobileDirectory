const fs = require('fs');
const path = require('path');

const userHome = "C:\\Users\\ravip";
console.log(`Searching for file containing '54d5c151' in: ${userHome}`);

function searchFile(dir, pattern) {
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
            // Avoid recursive traversal into heavy system/app folders unless necessary, but let's check AppData specifically if it matches
            if (file.toLowerCase() === 'appdata' || file.toLowerCase() === '.gemini' || file.toLowerCase() === 'android' || file.toLowerCase() === 'androidstudio' || file.toLowerCase() === 'workspace') {
                results = results.concat(searchFile(fullPath, pattern));
            } else if (file.startsWith('.') || file.toLowerCase() === 'node_modules' || file.toLowerCase() === 'appdata/local/temp') {
                // skip minor dot folders
            } else {
                results = results.concat(searchFile(fullPath, pattern));
            }
        } else {
            if (file.includes(pattern)) {
                results.push({ path: fullPath, size: stat.size });
            }
        }
    });
    return results;
}

const matches = searchFile(userHome, "54d5c151");
console.log(`Found ${matches.length} matches:`);
matches.forEach(m => {
    console.log(`- ${m.path} (${m.size} bytes)`);
});
