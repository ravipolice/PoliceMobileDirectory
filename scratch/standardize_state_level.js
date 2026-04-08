const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = 'ALL';
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

const genericUnits = ['HQ', 'State HQ', 'Administration', 'Crime', 'Statelevel', 'Admin', ''];

let updatedCount = 0;

data.forEach(row => {
    // Only target State Level records with generic unit values
    if (row.Range === 'State Level' && genericUnits.includes(row.Unit)) {
        const name = (row.Name || '').toLowerCase();
        const station = (row.Station || '').toLowerCase();
        
        // 1. Districts (Regional or specific cell offices)
        if (name.includes('division') || name.includes('cell') || name.includes('district') || station.includes('division')) {
            // Keep top leadership/admin as HQ
            if (name.includes('admin') || name.includes('chief') || name.includes('dg & igp')) {
                row.Unit = 'HQ';
            } else {
                row.Unit = 'Districts';
            }
            updatedCount++;
        }
        // 2. HQ (Everything else in State Level HQ)
        else {
            row.Unit = 'HQ';
            updatedCount++;
        }
    }
});

console.log(`Standardized ${updatedCount} generic State Level records into 'HQ' and 'Districts'.`);

const headers = ['agid', 'Section', 'Unit', 'Range', 'District', 'Sub Division', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2', 'searchBlob'];
const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h] || ''));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map(() => ({wch: 25}));
wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);

console.log('Successfully updated Excel. Refreshing all sheets...');
