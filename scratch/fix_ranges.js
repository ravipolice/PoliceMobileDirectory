const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath, { cellText: false, cellDates: true });

const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet, { range: 1, raw: false });

const expectedHeaders = ['Section', 'Unit', 'Range', 'District', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email'];
const headerSet = new Set();
data.forEach(row => Object.keys(row).forEach(k => headerSet.add(k)));
const headers = [...new Set([...expectedHeaders, ...Array.from(headerSet)])];

// Carry forward Range for Ranges section rows missing it
let lastRange = '';
let fixedCount = 0;

data.forEach(row => {
    if (row.Section === 'Ranges') {
        if (row.Range) {
            lastRange = row.Range; // Update the last known range
        } else if (lastRange) {
            row.Range = lastRange; // Fill blank Range from previous
            fixedCount++;
        }
    }
});

console.log(`Fixed Range for ${fixedCount} records.`);

// Rebuild sheet
const wsData = [['KSP Contact Directory - All Units (3791 records)'], headers];
data.forEach(row => wsData.push(headers.map(h => row[h])));

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = [
    {wch: 25}, {wch: 20}, {wch: 20}, {wch: 20},
    {wch: 30}, {wch: 20}, {wch: 25}, {wch: 15},
    {wch: 15}, {wch: 15}, {wch: 15}, {wch: 35}
];

wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);
console.log(`Successfully updated ${filePath}`);
