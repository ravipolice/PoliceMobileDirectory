const xlsx = require('xlsx');
const path = require('path');

const cleanPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3_CLEAN.xlsx');
const wb = xlsx.readFile(cleanPath);
const rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);

const targets = rows.filter(r => /Srikumar|Ajai|P.P.R.Nair|Balakrishnan/i.test(r.Name));
console.log(targets.map(r => ({
    agid: r.agid,
    Name: r.Name,
    Rank: r.Rank,
    UNIT: r.UNIT
})));
