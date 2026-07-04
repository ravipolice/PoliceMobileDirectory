const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const data = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED']);

console.log(`Total records in Master: ${data.length}`);

const keywords = ["Bagalkot", "Bagalkote", "Mudhol", "Jamkhandi", "Hunugund", "Ilkal", "Bilagi", "Guledagudda"];
const found = data.filter(r => {
    const s = JSON.stringify(r).toLowerCase();
    return keywords.some(k => s.includes(k.toLowerCase()));
});

console.log(`Global matches for Bagalkot keywords: ${found.length}`);
if (found.length > 0) {
    console.log('Sample match:', found[0]);
}
