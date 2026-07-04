const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

console.log('--- Verification of Official KSP Wings ---');
const wings = [...new Set(data.map(r => r.Wing))];
console.log('Detected Wings:', wings);

console.log('\n--- Sample Mapping ---');
const sample = data.filter(r => r.Wing !== 'L&O').slice(0, 15);
console.table(sample.map(r => ({
    Wing: r.Wing,
    Unit: r.Unit,
    Section: r.Section,
    Station: r.Station,
    Name: r.Name
})));
