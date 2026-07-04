const fs = require('fs');
const path = require('path');

const workspaceDir = "c:\\Users\\ravip\\AndroidStudioProjects";
console.log(`Searching workspace: ${workspaceDir}`);

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
            if (file.startsWith('.') || file.toLowerCase() === 'node_modules' || file.toLowerCase() === 'build') {
                // skip heavy build and node folders
            } else {
                results = results.concat(walk(fullPath));
            }
        } else {
            const ext = path.extname(file).toLowerCase();
            if (ext === '.png' || ext === '.jpg' || ext === '.jpeg') {
                if (stat.mtimeMs >= tenMinutesAgo) {
                    results.push({ path: fullPath, size: stat.size, mtime: stat.mtime });
                }
            }
        }
    });
    return results;
}

if (fs.existsSync(workspaceDir)) {
    const files = walk(workspaceDir);
    console.log(`Found ${files.length} recently modified images in workspace:`);
    files.forEach(f => {
        console.log(`- ${f.path} (${f.size} bytes) - Modified: ${f.mtime.toISOString()}`);
    });
} else {
    console.log("Workspace directory does not exist!");
}
