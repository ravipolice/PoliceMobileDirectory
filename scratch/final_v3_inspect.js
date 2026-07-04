const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const data = xlsx.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]]);

console.log('Total Rows:', data.length);
console.log('Districts:', [...new Set(data.map(r => r.District))]);
