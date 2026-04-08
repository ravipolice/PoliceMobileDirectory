const xlsx = require('xlsx');

const finalPath = '../KSP_Contacts_UnitWise_Final.xlsx';
const sourcePath = '../KSP_Contacts_UnitWise.xlsx';

const finalWb = xlsx.readFile(finalPath);
const sourceWb = xlsx.readFile(sourcePath);

const finalData = xlsx.utils.sheet_to_json(finalWb.Sheets[finalWb.SheetNames[0]]);
const sourceData = xlsx.utils.sheet_to_json(sourceWb.Sheets[sourceWb.SheetNames[0]], {range: 1});

// Create a map of the source data for quick lookup
const sourceMap = new Map();
sourceData.forEach(row => {
    // Generate a key from available fields. Using Name, Rank, Station as they were used to generate Name.
    // Wait, sourceData might have old Name values. Let's use Section, Unit, Rank, Station.
    const key = `${row.Section}|${row.Unit}|${row.Rank}|${row.Station}`;
    sourceMap.set(key, row);
});

let restoredCount = 0;

finalData.forEach(row => {
    // Try to find the matching row in the source data to get Office 1 and Office 2
    // Note: row.Name in finalData is Rank + Station.
    // Let's use the same key components.
    const key = `${row.Section}|${row.Unit}|${row.Rank}|${row.Station}`;
    const sourceRow = sourceMap.get(key);
    
    if (sourceRow) {
        row['Office 1'] = sourceRow['Office 1'];
        row['Office 2'] = sourceRow['Office 2'];
        restoredCount++;
    }
});

console.log(`Restored Office columns for ${restoredCount} records.`);

const headers = ['Section', 'Unit', 'Range', 'District', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2'];

const wsData = [headers];
finalData.forEach(row => {
    wsData.push(headers.map(h => row[h]));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map(() => ({wch: 20})); // Default width

finalWb.Sheets[finalWb.SheetNames[0]] = newSheet;
xlsx.writeFile(finalWb, finalPath);
console.log(`Successfully restored and updated ${finalPath}`);
