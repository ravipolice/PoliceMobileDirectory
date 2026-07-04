const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);

console.log('--- FINAL V3 STATUS ---');
console.log(`Total Sheets: ${wb.SheetNames.length}`);
wb.SheetNames.forEach(name => {
    const data = xlsx.utils.sheet_to_json(wb.Sheets[name]);
    console.log(`${name}: ${data.length} rows`);
});
