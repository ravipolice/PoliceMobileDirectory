const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const data = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED']);

console.log('--- DISTRICT VERIFICATION IN MASTER ---');
const bagalkotInMaster = data.filter(r => r.District === 'Bagalkot');
console.log(`Bagalkot records in Master: ${bagalkotInMaster.length}`);

if (bagalkotInMaster.length > 0) {
    console.log('Sample Bagalkot Record:');
    console.table([bagalkotInMaster[0]].map(r => ({
        Name: r.Name, 
        Unit: r.Unit, 
        Range: r.Range, 
        District: r.District
    })));
}

const totalMissingRange = data.filter(r => !r.Range).length;
console.log(`\nTotal records with missing Range: ${totalMissingRange}`);
const missingRangeByUnit = {};
data.filter(r => !r.Range).forEach(r => {
    missingRangeByUnit[r.Unit] = (missingRangeByUnit[r.Unit] || 0) + 1;
});
console.log('Units with missing Range:');
console.log(missingRangeByUnit);
