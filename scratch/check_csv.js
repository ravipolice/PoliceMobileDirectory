const fs = require('fs');
const path = require('path');

const csvPath = path.join(__dirname, '..', 'KSP_Contacts_Master_Clean.csv');
const content = fs.readFileSync(csvPath, 'utf8');
const lines = content.split('\n');

console.log("CSV Rows with AAO:");
lines.forEach((line, index) => {
    if (line.includes("AAO")) {
        console.log(`${index}: ${line}`);
    }
});
