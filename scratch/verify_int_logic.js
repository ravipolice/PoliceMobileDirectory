const xlsx = require('xlsx');
const workbook = xlsx.readFile('KSP_Contacts_Final_Directory_V3.xlsx');
const sheet = workbook.Sheets['MASTER_MERGED_FINAL'];
const data = xlsx.utils.sheet_to_json(sheet);
const intRecords = data.filter(r => r.UNIT === 'Intelligence').slice(0, 20);
console.log(JSON.stringify(intRecords, null, 2));
