const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

const sdRecords = data.filter(r => r['Sub Division'] && r['Sub Division'] !== '');

console.log('--- Verification of Sub Division Extraction ---');
console.log(`Total records with Sub Division: ${sdRecords.length}`);
if (sdRecords.length > 0) {
    console.table(sdRecords.slice(0, 10).map(r => ({ 
        'Sub Division': r['Sub Division'], 
        Unit: r.Unit,
        Name: r.Name 
    })));
}
