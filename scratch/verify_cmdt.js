const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

const cmdtRecords = data.filter(r => r.Rank && (r.Rank.includes('Cmdt.') || r.Rank === 'Cmdt.'));

console.log('--- Verification of Commandant Short Codes ---');
console.log(`Total records with Cmdt short codes: ${cmdtRecords.length}`);
if (cmdtRecords.length > 0) {
    console.table(cmdtRecords.slice(0, 10).map(r => ({ 
        Rank: r.Rank, 
        Unit: r.Unit,
        Name: r.Name 
    })));
}
