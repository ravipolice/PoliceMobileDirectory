const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

const fpbRecords = data.filter(r => r.Section === 'FPB' && r.District);

console.log('--- FPB Range Verification ---');
if (fpbRecords.length > 0) {
    console.table(fpbRecords.slice(0, 15).map(r => ({
        Unit: r.Unit,
        District: r.District,
        Range: r.Range,
        Name: r.Name
    })));
} else {
    console.log('No FPB records with districts found!');
}
