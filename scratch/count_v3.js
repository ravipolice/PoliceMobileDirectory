const xlsx = require('xlsx');
const path = require('path');
const wb = xlsx.readFile(path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx'));
const masterSheet = wb.Sheets['MASTER_MERGED_FINAL'];
if (!masterSheet) {
    console.log("MASTER_MERGED_FINAL sheet not found. Sheet names:", wb.SheetNames);
} else {
    const data = xlsx.utils.sheet_to_json(masterSheet);
    console.log("Total rows in MASTER_MERGED_FINAL:", data.length);
}
