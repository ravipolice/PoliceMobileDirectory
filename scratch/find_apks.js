const fs = require('fs');
const path = require('path');

function findApks(dir) {
    let results = [];
    const list = fs.readdirSync(dir);
    list.forEach(file => {
        const fullPath = path.join(dir, file);
        const stat = fs.statSync(fullPath);
        if (stat && stat.isDirectory()) {
            if (file !== 'node_modules' && file !== '.git' && file !== '.gradle') {
                results = results.concat(findApks(fullPath));
            }
        } else if (file.endsWith('.apk')) {
            results.push(fullPath);
        }
    });
    return results;
}

const apks = findApks(path.join(__dirname, '..'));
console.log(`Found ${apks.length} APK files:`);
apks.forEach(apk => console.log(apk));
