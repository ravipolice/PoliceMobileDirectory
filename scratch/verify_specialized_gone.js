const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

console.log('--- Targeted Wing Check (HCD & FES) ---');
const sample = data.filter(r => r.Unit === 'HCD & FES').slice(0, 5);
console.table(sample.map(r => ({
    Wing: r.Wing,
    Unit: r.Unit,
    Section: r.Section,
    Station: r.Station,
    Name: r.Name
})));

const specializedCount = data.filter(r => r.Wing === 'Specialized').length;
console.log(`\nRemaining "Specialized" records: ${specializedCount}`);
