const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const data = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);

console.log('--- TOP SORT VERIFICATION ---');
console.log(data.slice(0, 10).map((r, i) => `${i+1}. ${r.Rank} - ${r.Name} (${r['Mobile 1']})`));
