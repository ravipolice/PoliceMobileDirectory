const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);

console.log('--- EXACT SHEET NAMES ---');
wb.SheetNames.forEach((name, i) => {
    console.log(`${i}: [${name}] (Hex: ${Buffer.from(name).toString('hex')})`);
});
