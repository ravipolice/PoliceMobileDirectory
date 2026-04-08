const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

let subDivisionFound = 0;

data.forEach(row => {
    row['Sub Division'] = ''; // Initialize column
    
    if (row.Name) {
        // Look for keywords like "Sub Division", "SDPO", "Sub-Division"
        const match = row.Name.match(/(.+?)\s+(Sub[- ]Division|SDPO)/i);
        if (match) {
            row['Sub Division'] = match[1].trim();
            subDivisionFound++;
        }
    }
});

console.log(`Identified ${subDivisionFound} records with explicit Sub Division info.`);

// New header order
const headers = ['agid', 'Section', 'Unit', 'Range', 'District', 'Sub Division', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2', 'searchBlob'];
const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h] || ''));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map(() => ({wch: 25}));
wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);
console.log(`Successfully updated ${filePath} with Sub Division column.`);
