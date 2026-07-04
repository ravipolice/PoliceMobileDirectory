const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

const rangeRecords = data.filter(r => r.Category === 'RANGES' || r.Section?.includes('Range'));

console.log('--- Range Data Verification ---');
console.log(`Total records checked: ${rangeRecords.length}`);
if (rangeRecords.length > 0) {
    console.table(rangeRecords.slice(0, 10).map(r => ({
        Unit: r.Unit,
        Range: r.Range,
        District: r.District,
        Station: r.Station
    })));
} else {
    console.log('No Range records found in the current Excel!');
}
