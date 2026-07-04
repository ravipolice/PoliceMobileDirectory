const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);

console.log('Total Sheets:', wb.SheetNames.length);
console.log('Sheet Names:', wb.SheetNames);
