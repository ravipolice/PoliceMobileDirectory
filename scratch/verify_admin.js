const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

const adminRanks = ['PA', 'AAO', 'AO', 'CAO', 'OS', 'Supt.', 'Manager'];
const adminRecords = data.filter(r => adminRanks.includes(r.Rank));

console.log('--- Verification of Administrative Ranks ---');
console.log(`Total administrative records found: ${adminRecords.length}`);
if (adminRecords.length > 0) {
    console.table(adminRecords.slice(0, 15).map(r => ({ 
        Rank: r.Rank, 
        Unit: r.Unit,
        Name: r.Name,
        Station: r.Station
    })));
}
