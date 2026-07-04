const xlsx = require('xlsx');
const workbook = xlsx.readFile('KSP_Contacts_Final_Directory_V3_CLEAN.xlsx');
const sheet = workbook.Sheets['MASTER_MERGED_FINAL'];
const data = xlsx.utils.sheet_to_json(sheet);
const retdRecords = data.filter(r => r.Rank.includes('RETD')).slice(0, 5);
console.log(JSON.stringify(retdRecords, null, 2));
