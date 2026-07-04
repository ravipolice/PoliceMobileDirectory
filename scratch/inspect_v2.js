const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V2.xlsx');
const wb = xlsx.readFile(filePath);

console.log(`V2 Sheet Count: ${wb.SheetNames.length}`);
const bagalkot = wb.Sheets['Bagalkot'];
if (bagalkot) {
    const data = xlsx.utils.sheet_to_json(bagalkot);
    console.log(`Bagalkot Rows in V2: ${data.length}`);
} else {
    console.log('Bagalkot sheet not found in V2');
}
