const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

console.log('--- Verification of Range vs Sub Division (Central) ---');
const sample = data.filter(r => r.Name.includes('Central Range') || r.Name.includes('Northern Range') || r.Name.includes('Southern Range'));

console.table(sample.map(r => ({
    Range: r.Range,
    SubDiv: r['Sub Division'],
    Name: r.Name
})));
