const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

let replacedCount = 0;

data.forEach(row => {
    const fields = ['Section', 'Unit', 'Range', 'District', 'Name', 'Rank', 'Station'];
    fields.forEach(f => {
        if (row[f] && typeof row[f] === 'string' && row[f].includes('Department of ')) {
            row[f] = row[f].replace(/Department of\s+/gi, '').trim();
            replacedCount++;
        }
    });
});

console.log(`Removed "Department of" in ${replacedCount} instances.`);

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
