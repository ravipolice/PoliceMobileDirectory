const xlsx = require('xlsx');
const path = require('path');

const cleanPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3_CLEAN.xlsx');
const wb = xlsx.readFile(cleanPath);
const rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);

const targets = rows.filter(r => ['AGID4595', 'AGID4487', 'AGID3334', 'AGID3459', 'AGID3464', 'AGID4285', 'AGID3328'].includes(r.agid));
console.log(targets.map(r => ({
    agid: r.agid,
    Name: r.Name,
    Rank: r.Rank,
    UNIT: r.UNIT
})));
