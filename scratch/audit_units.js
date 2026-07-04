const xlsx = require('xlsx');
const workbook = xlsx.readFile('KSP_Contacts_Final_Directory_V3.xlsx');
const sheet = workbook.Sheets['MASTER_MERGED_FINAL'];
const data = xlsx.utils.sheet_to_json(sheet);
const units = [...new Set(data.map(r => r.UNIT))].sort();
console.log('Unique Units found:');
console.log(JSON.stringify(units, null, 2));
