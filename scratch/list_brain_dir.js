const fs = require('fs');
const path = require('path');

const brainDir = "C:\\Users\\ravip\\.gemini\\antigravity\\brain\\cc2a4ac8-491f-4ef1-8e68-260091d4b65b";
console.log(`Searching brain directory: ${brainDir}`);

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
            results.push({ path: fullPath, size: stat.size });
        }
    });
    return results;
}

if (fs.existsSync(brainDir)) {
    const files = walk(brainDir);
    console.log(`Found ${files.length} files:`);
    files.forEach(f => {
        console.log(`- ${f.path} (${f.size} bytes)`);
    });
} else {
    console.log("Brain directory does not exist!");
}
