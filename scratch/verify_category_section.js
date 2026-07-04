const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

console.log('--- Verification of Category vs Section (Work Type) ---');
const sample = data.filter(r => r.Category === 'CHIEF OFFICE' || r.Category === 'SPHQ').slice(0, 10);

console.table(sample.map(r => ({
    Category: r.Category,
    Section: r.Section,
    Unit: r.Unit,
    Name: r.Name,
    Rank: r.Rank
})));

const psSample = data.filter(r => r.Category === 'COMMISSIONERATES' && r.Station.includes('PS')).slice(0, 5);
if (psSample.length > 0) {
    console.log('\n--- Police Station Samples ---');
    console.table(psSample.map(r => ({
        Category: r.Category,
        Section: r.Section,
        Unit: r.Unit,
        Station: r.Station,
        Name: r.Name
    })));
}
