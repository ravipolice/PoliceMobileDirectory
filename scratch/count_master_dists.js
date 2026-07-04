const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const data = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);

const withDist = data.filter(r => r.District && String(r.District).trim() !== "");
console.log('Rows with District:', withDist.length);
if (withDist.length > 0) {
    console.log('Sample District Row:', withDist[0]);
}

const distCounts = {};
withDist.forEach(r => distCounts[r.District] = (distCounts[r.District] || 0) + 1);
console.log('District Counts:', distCounts);
