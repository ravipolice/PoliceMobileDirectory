const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);

const sheet = wb.Sheets['Bagalkot'];
const data = xlsx.utils.sheet_to_json(sheet);

console.log(`Rows: ${data.length}`);
if (data.length > 0) {
    console.log('Keys in first row:', Object.keys(data[0]));
    console.log('Sample Row:', data[0]);
}
