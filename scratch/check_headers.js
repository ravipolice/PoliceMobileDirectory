const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);

const master = wb.Sheets['MASTER_MERGED'];
const district = wb.Sheets['Ballari'];

const masterRow = xlsx.utils.sheet_to_json(master)[0];
const districtRow = xlsx.utils.sheet_to_json(district)[0];

console.log('Master Keys:', Object.keys(masterRow));
console.log('District Keys:', Object.keys(districtRow));
