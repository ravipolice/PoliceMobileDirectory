const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const data = xlsx.utils.sheet_to_json(wb.Sheets['Northern Range – Belagavi']);

const counts = {};
data.forEach(r => counts[r.District] = (counts[r.District] || 0) + 1);
console.log('--- FINAL NORTHERN RANGE COUNTS ---');
console.log(counts);
console.log('Total in Northern Range Sheet:', data.length);
