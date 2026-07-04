const fs = require('fs');
const path = require('path');

const csvPath = path.join(__dirname, '..', 'KSP_Contacts_Master_Clean.csv');
if (!fs.existsSync(csvPath)) {
    console.error("CSV file does not exist");
    process.exit(1);
}

const content = fs.readFileSync(csvPath, 'utf8');
const lines = content.split('\n');

console.log(`CSV has ${lines.length} lines.`);
let found = 0;
lines.forEach((line, idx) => {
    if (line.toLowerCase().includes('haveri') || line.toLowerCase().includes('shig')) {
        found++;
        if (found <= 20) {
            console.log(`Line ${idx+1}: ${line}`);
        }
    }
});
console.log(`Found ${found} lines.`);
