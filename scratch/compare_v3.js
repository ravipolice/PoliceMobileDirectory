const xlsx = require('xlsx');
const fs = require('fs');
const path = require('path');

const v3Path = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const currentDataPath = path.join(__dirname, '..', 'officers_v2_current.json');

const currentData = JSON.parse(fs.readFileSync(currentDataPath, 'utf8'));
const wb = xlsx.readFile(v3Path);
const v3Rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);

console.log("--- Sample from Firestore ---");
currentData.slice(0, 5).forEach(r => {
    console.log(`${r.agid}: ${r.name} | ${r.rank} | ${r.mobile}`);
});

console.log("\n--- Sample from V3 Excel ---");
v3Rows.slice(0, 5).forEach(r => {
    console.log(`${r.Name} | ${r.Rank} | ${r['mobile 1']}`);
});
