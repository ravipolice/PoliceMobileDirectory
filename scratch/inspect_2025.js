const xlsx = require('xlsx');
const path = require('path');

const file2025 = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_2025.xlsx');
const wb = xlsx.readFile(file2025);

console.log(`2025 Sheet Count: ${wb.SheetNames.length}`);
console.log('Sheet Names:', wb.SheetNames.slice(0, 10).join(', ') + '...');

const bagalkot = wb.Sheets['Bagalkot'];
if (bagalkot) {
    const data = xlsx.utils.sheet_to_json(bagalkot);
    console.log(`Bagalkot Rows in 2025: ${data.length}`);
}
