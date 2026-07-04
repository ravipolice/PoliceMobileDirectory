const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

console.log('--- Verification of STD Codes ---');
const sample = data.filter(r => r['Office 1'] && r['Office 1'].includes('-')).slice(0, 15);

console.table(sample.map(r => ({
    District: r.District,
    Unit: r.Unit,
    Office1: r['Office 1']
})));
