const xlsx = require('xlsx');
const path = require('path');

const cleanPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3_CLEAN.xlsx');
if (require('fs').existsSync(cleanPath)) {
    const wb = xlsx.readFile(cleanPath);
    const rows = xlsx.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]]);
    console.log(`Rows in clean excel: ${rows.length}`);
    console.log('Sample rows (first 10):');
    console.log(rows.slice(0, 10).map(r => r.Name));
} else {
    console.log('Clean excel not found!');
}
