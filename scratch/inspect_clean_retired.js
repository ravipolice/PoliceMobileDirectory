const xlsx = require('xlsx');
const path = require('path');

const cleanPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3_CLEAN.xlsx');
const wb = xlsx.readFile(cleanPath);
const rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);

const retiredRows = rows.filter(r => /Retired/i.test(r.UNIT) || /Retired/i.test(r.District) || /retd/i.test(r.Rank));
console.log(`Retired rows in clean: ${retiredRows.length}`);
console.log(retiredRows.map(r => ({
    Name: r.Name,
    Rank: r.Rank
})));
