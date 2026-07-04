const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);

const masterSheet = wb.Sheets['MASTER_MERGED'];
const masterData = xlsx.utils.sheet_to_json(masterSheet);
console.log(`Total rows in MASTER_MERGED: ${masterData.length}`);

let totalOthers = 0;
wb.SheetNames.forEach(name => {
    if (name !== 'MASTER_MERGED') {
        const data = xlsx.utils.sheet_to_json(wb.Sheets[name]);
        totalOthers += data.length;
    }
});

console.log(`Total rows combined from all other sheets: ${totalOthers}`);

if (masterData.length < totalOthers) {
    console.log('WARNING: MASTER_MERGED is missing records!');
} else if (masterData.length > totalOthers) {
    console.log('NOTE: MASTER_MERGED has more records than the sum of other sheets (might have duplicates or old data).');
}
