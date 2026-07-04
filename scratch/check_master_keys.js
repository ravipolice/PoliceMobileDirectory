const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const data = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);

console.log('Sample Data from Master:', data.slice(0, 5));
console.log('Keys in data:', Object.keys(data[0] || {}));
