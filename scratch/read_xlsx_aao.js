const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);

const aaos = rows.filter(r => String(r.Rank).toUpperCase().trim() === 'AAO');

console.log(`Found ${aaos.length} AAO rows in V3 Excel:`);
aaos.forEach((r, idx) => {
    console.log(`Row ${idx+1}: Name="${r.Name}" | Rank="${r.Rank}" | UNIT="${r.UNIT}" | Range="${r.Range}" | District="${r.District}" | Section="${r.Section}" | email1="${r.email1}"`);
});
