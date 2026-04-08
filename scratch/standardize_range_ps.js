const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

let updatedCount = 0;

data.forEach(row => {
    // 1. Target records in Ranges
    if (row.Section === 'Ranges') {
        // 2. If it's a Police Station (PS)
        const isPS = (row.Station && row.Station.includes('PS')) || 
                     (row.Name && row.Name.includes('PS')) ||
                     (row.Name && row.Name.toLowerCase().includes('police station'));
        
        if (isPS) {
            // 3. Update Unit to L&O if it's currently a range name
            if (row.Unit && row.Unit.includes('Range')) {
                row.Unit = 'L&O';
                updatedCount++;
            }
        }
    }
});

console.log(`Updated ${updatedCount} PS records in Ranges to have "L&O" as their Unit.`);

const headers = ['agid', 'Section', 'Unit', 'Range', 'District', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2', 'searchBlob'];
const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h] || ''));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map(() => ({wch: 25}));
wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);
console.log(`Successfully updated ${filePath}`);
