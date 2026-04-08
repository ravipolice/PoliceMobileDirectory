const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

let replacedCount = 0;

data.forEach(row => {
    // Standardize Rank column
    if (row.Rank === 'PA to') {
        row.Rank = 'PA';
        replacedCount++;
    }
    
    // Standardize Name field
    if (row.Name && (row.Name.startsWith('PA to') || row.Name.startsWith('PA TO'))) {
        row.Name = row.Name.replace(/^PA\s+to/i, 'PA');
        replacedCount++;
    }
});

console.log(`Simplified "PA to" to "PA" in ${replacedCount} instances.`);

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
