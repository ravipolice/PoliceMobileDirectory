const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const data = xlsx.utils.sheet_to_json(wb.Sheets['Northern Range – Belagavi']);

const dists = {};
data.forEach(r => dists[r.District] = (dists[r.District] || 0) + 1);
console.log('Districts in Northern Range:', dists);
