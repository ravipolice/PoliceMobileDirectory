const xlsx = require('xlsx');

const finalPath = '../KSP_Contacts_UnitWise_Final.xlsx';
const sourcePath = '../KSP_Contacts_UnitWise.xlsx';

const finalWb = xlsx.readFile(finalPath);
const sourceWb = xlsx.readFile(sourcePath);

const finalData = xlsx.utils.sheet_to_json(finalWb.Sheets[finalWb.SheetNames[0]]);
const sourceData = xlsx.utils.sheet_to_json(sourceWb.Sheets[sourceWb.SheetNames[0]], {range: 1});

const sourceMap = new Map();
sourceData.forEach(row => {
    const key = `${row.Section}|${row.Unit}|${row.Rank}|${row.Station}`;
    sourceMap.set(key, row);
});

let emailSplitCount = 0;

finalData.forEach(row => {
    const key = `${row.Section}|${row.Unit}|${row.Rank}|${row.Station}`;
    const sourceRow = sourceMap.get(key);
    
    if (sourceRow) {
        row['Office 1'] = sourceRow['Office 1'];
        row['Office 2'] = sourceRow['Office 2'];
        
        // Also redo the email split from source data
        if (sourceRow.Email) {
            const parts = sourceRow.Email.split(/[,;\/]/).map(e => e.trim()).filter(e => e.includes('@'));
            if (parts.length >= 2) {
                row.Email = parts[0];
                row.Email2 = parts[1];
                emailSplitCount++;
            } else {
                row.Email = sourceRow.Email;
                row.Email2 = '';
            }
        }
    }
});

console.log(`Restored columns and split ${emailSplitCount} dual emails.`);

const headers = ['Section', 'Unit', 'Range', 'District', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2'];
const wsData = [headers];
finalData.forEach(row => {
    wsData.push(headers.map(h => row[h] || ''));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map((h, i) => ({wch: [20, 20, 20, 20, 35, 20, 25, 15, 15, 15, 15, 30, 30][i]}));

finalWb.Sheets[finalWb.SheetNames[0]] = newSheet;
xlsx.writeFile(finalWb, finalPath);
console.log(`Final update completed for ${finalPath}`);
