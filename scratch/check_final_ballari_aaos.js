const xlsx = require('xlsx');
const path = require('path');

const xlsPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const workbook = xlsx.readFile(xlsPath);
const sheetName = workbook.SheetNames[0];
const sheet = workbook.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

const ballariAaos = data.filter(row => {
    const unit = String(row.UNIT || "");
    const name = String(row.Name || "");
    const dist = String(row.District || "");
    return (unit.includes("Ballari") || dist.includes("Ballari")) && (row.Rank === "AAO" || name.includes("AAO"));
});

console.log("Ballari AAOs in Excel:");
console.log(JSON.stringify(ballariAaos, null, 2));
