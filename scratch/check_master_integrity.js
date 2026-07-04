const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const data = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED']);

console.log('--- MASTER_MERGED Integrity Check ---');
console.log(`Total rows: ${data.length}`);
console.log(`Empty names: ${data.filter(r => !r.Name).length}`);
console.log(`Empty mobile/office numbers: ${data.filter(r => !r['Mobile 1'] && !r['Office 1']).length}`);
console.log(`Missing Unit/Range: ${data.filter(r => !r.Unit || !r.Range).length}`);

console.log('\nSample Rows:');
console.table(data.slice(0, 10).map(r => ({Name: r.Name, Unit: r.Unit, Mobile: r['Mobile 1']})));
