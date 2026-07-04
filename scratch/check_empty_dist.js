const xlsx = require('xlsx');
const path = require('path');

const wb = xlsx.readFile(path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx'));
const rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);
console.log('Total rows:', rows.length);
console.log('Empty districts:', rows.filter(r => !r.District).length);
const sampleEmpty = rows.filter(r => !r.District).slice(0, 5);
console.log('Sample empty:', JSON.stringify(sampleEmpty, null, 2));
process.exit(0);
