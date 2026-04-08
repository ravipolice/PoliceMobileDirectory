const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise.xlsx';
const workbook = xlsx.readFile(filePath);

const sheetName = workbook.SheetNames[0];
const worksheet = workbook.Sheets[sheetName];

const data = xlsx.utils.sheet_to_json(worksheet);

// Let's print the first row to see columns
console.log("Columns:", Object.keys(data[0] || {}));

// Let's collect unique Ranges and Commissionerates
const ranges = new Set();
const commissionerates = new Set();
const districts = new Set();

data.forEach(row => {
    if (row.Range) ranges.add(row.Range);
    if (row.Commissionerate) commissionerates.add(row.Commissionerate);
    // Alternatively, if it's just 'District' and 'Unit'
    if (row.district) districts.add(row.district);
    if (row.unit) commissionerates.add(row.unit);
});

console.log("\nRanges:");
console.log(Array.from(ranges).sort());

console.log("\nCommissionerates:");
console.log(Array.from(commissionerates).sort());

console.log("\nDistricts/Regions:");
console.log(Array.from(districts).sort());
