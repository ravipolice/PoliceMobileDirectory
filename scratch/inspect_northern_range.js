const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);

const sheet = wb.Sheets['Northern Range – Belagavi'];
const data = xlsx.utils.sheet_to_json(sheet);

console.log(`Northern Range rows: ${data.length}`);
const dists = {};
data.forEach(r => dists[r.District] = (dists[r.District] || 0) + 1);
console.log('Districts in Northern Range sheet:', dists);

if (data.length > 0) {
    console.log('Sample Row keys:', Object.keys(data[0]));
    console.log('Sample Row:', data[0]);
}
