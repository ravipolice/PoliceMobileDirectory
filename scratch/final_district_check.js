const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);

const northern = wb.Sheets['Northern Range – Belagavi'];
const data = xlsx.utils.sheet_to_json(northern);

const bagalkotCount = data.filter(r => String(r.District).includes('Bagalkot')).length;
const gadagCount = data.filter(r => String(r.District).includes('Gadag')).length;
const dharwadCount = data.filter(r => String(r.District).includes('Dharwad')).length;

console.log(`Bagalkot records: ${bagalkotCount}`);
console.log(`Gadag records: ${gadagCount}`);
console.log(`Dharwad records: ${dharwadCount}`);
