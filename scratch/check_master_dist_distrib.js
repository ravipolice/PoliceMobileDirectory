const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const data = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED']);

console.log('Sample Row 0:', data[0]);
console.log('Sample Row 1000:', data[1000]);
console.log('Sample Row 2000:', data[2000]);

const distCounts = {};
data.forEach(r => distCounts[r.District] = (distCounts[r.District] || 0) + 1);
console.log('District Distribution in Master:', distCounts);
