const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

const stateLevelRecords = data.filter(r => r.District === 'State Level' || r.Range === 'State Level');

console.log('--- Verification of Blanking ---');
console.log(`Records with "State Level" text remaining: ${stateLevelRecords.length}`);
if (stateLevelRecords.length > 0) {
    console.log('Sample of remaining "State Level" text:');
    console.table(stateLevelRecords.slice(0, 5));
} else {
    console.log('SUCCESS: All "State Level" placeholders are now blank.');
}

const totalRecords = data.length;
const blankDistrict = data.filter(r => !r.District || r.District === '').length;
const blankRange = data.filter(r => !r.Range || r.Range === '').length;

console.log(`\nTotal Records: ${totalRecords}`);
console.log(`Blank Districts: ${blankDistrict}`);
console.log(`Blank Ranges: ${blankRange}`);
