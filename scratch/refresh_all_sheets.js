const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const masterSheetName = 'ALL';
const masterSheet = wb.Sheets[masterSheetName];

if (!masterSheet) {
    console.error('Master sheet "ALL" not found!');
    process.exit(1);
}

const data = xlsx.utils.sheet_to_json(masterSheet);
const units = [...new Set(data.map(r => r.Unit))].filter(u => u).sort();

console.log(`Refreshing ${units.length} unit sheets from master data...`);

// Create a new workbook to start fresh (keeps the ALL sheet first)
const newWb = xlsx.utils.book_new();

// Add the ALL sheet first
xlsx.utils.book_append_sheet(newWb, masterSheet, masterSheetName);

// Add each unit-wise sheet
units.forEach(unit => {
    const unitData = data.filter(r => r.Unit === unit);
    if (unitData.length === 0) return;
    
    const ws = xlsx.utils.json_to_sheet(unitData);
    
    // Sheet names have a 31 char limit and some illegal chars
    let sheetName = unit.replace(/[\\\/\?\*\[\]:]/g, '-').trim();
    if (sheetName.length > 31) sheetName = sheetName.substring(0, 28) + '...';
    
    // Ensure unique sheet names (just in case of truncation collisions)
    if (newWb.SheetNames.includes(sheetName)) {
        sheetName = sheetName.substring(0, 25) + '_' + Math.floor(Math.random() * 1000);
    }
    
    xlsx.utils.book_append_sheet(newWb, ws, sheetName);
});

xlsx.writeFile(newWb, filePath);
console.log(`Successfully refreshed complete Excel file with ${newWb.SheetNames.length} sheets.`);
