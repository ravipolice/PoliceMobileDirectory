const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

const intel = data.filter(r => r.Section === 'C/Room' || r.Unit === 'C/Room');
console.log('--- Control Room Sample ---');
console.table(intel.slice(0, 5).map(r => ({ Section: r.Section, Unit: r.Unit, Range: r.Range })));
