const xlsx = require('xlsx');
const path = require('path');

const file2025 = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_2025.xlsx');
const wb = xlsx.readFile(file2025);
const data = xlsx.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]]);

console.log('Total Rows in 2025:', data.length);
const distCounts = {};
data.forEach(r => distCounts[r.District] = (distCounts[r.District] || 0) + 1);
console.log('District Distribution in 2025:', distCounts);
