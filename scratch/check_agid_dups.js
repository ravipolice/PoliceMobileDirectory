const xlsx = require('xlsx');
const path = require('path');

const wb = xlsx.readFile(path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx'));
const rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);
const agids = rows.map(r => r.agid);
const uniqueAgids = [...new Set(agids)];

console.log('Total Rows:', rows.length);
console.log('Unique AGIDs:', uniqueAgids.length);

if (rows.length !== uniqueAgids.length) {
    const counts = {};
    agids.forEach(a => counts[a] = (counts[a] || 0) + 1);
    const dups = Object.keys(counts).filter(a => counts[a] > 1);
    console.log('Duplicate AGIDs count:', dups.length);
    console.log('Sample duplicates:', dups.slice(0, 5));
}
process.exit(0);
