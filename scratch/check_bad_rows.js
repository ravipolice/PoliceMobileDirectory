const xlsx = require('xlsx');
const path = require('path');

const wb = xlsx.readFile(path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx'));
const rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);
const badRows = rows.filter(r => r.Name === r.Rank);
console.log('Total rows where Name equals Rank:', badRows.length);
console.log('Sample:', JSON.stringify(badRows.slice(0, 10), null, 2));
process.exit(0);
