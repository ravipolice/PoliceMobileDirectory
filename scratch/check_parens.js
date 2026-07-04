const xlsx = require('xlsx');
const path = require('path');

const wb = xlsx.readFile(path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx'));
const rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);
const parenRows = rows.filter(r => r.Name.includes('('));
console.log('Total rows with ( in Name:', parenRows.length);
console.log('Sample:', JSON.stringify(parenRows.slice(0, 10).map(r => r.Name), null, 2));
process.exit(0);
