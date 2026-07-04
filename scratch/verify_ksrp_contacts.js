const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets['KSRP']; // Using the unit-wise sheet
const data = xlsx.utils.sheet_to_json(sheet);

console.log('--- KSRP Battalion Contacts Verification ---');
const sample = data.filter(r => r.Name.includes('Commandant') || r.Name.includes('Bn') || r.Name.includes('Battalion'));

console.table(sample.map(r => ({
    Unit: r.Unit,
    Name: r.Name,
    Rank: r.Rank,
    Office: r['Office 1']
})));
