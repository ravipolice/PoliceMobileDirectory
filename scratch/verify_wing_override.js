const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

console.log('--- Verification of Wing Keyword Override (CID/ISD) ---');
const sample = data.filter(r => r.Name.includes('CID') || r.Name.includes('ISD')).slice(0, 10);

console.table(sample.map(r => ({
    Wing: r.Wing,
    Section: r.Section,
    Station: r.Station,
    Name: r.Name
})));
