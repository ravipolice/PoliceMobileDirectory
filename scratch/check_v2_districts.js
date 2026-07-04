const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V2.xlsx');
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const data = xlsx.utils.sheet_to_json(wb.Sheets[sheetName]);

console.log(`Sheet Name: ${sheetName}`);
console.log(`Row count: ${data.length}`);
const districts = [...new Set(data.map(r => r.District))].sort();
console.log('Districts found:', districts);
