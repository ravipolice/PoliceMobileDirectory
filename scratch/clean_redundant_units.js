const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = 'ALL';
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

let updatedCount = 0;

data.forEach(row => {
    if (row.Unit && row.Unit.includes('Range')) {
        const name = row.Name || '';
        const station = row.Station || '';
        
        // 1. Range Headquarters
        if (name.includes('IGP') || name.includes('DIGP') || name.includes('Range Office') || name.includes('AAO') || name.includes('PA ')) {
            row.Unit = 'Range HQ';
            updatedCount++;
        }
        // 2. Law & Order (Circles and PS)
        else if (name.includes('Circle') || name.includes('CPI') || name.includes('PS') || station.includes('PS')) {
            row.Unit = 'L&O';
            updatedCount++;
        }
        // 3. Fallback for redundant Range names in Unit field
        else {
            // If it has a District, it's likely L&O or District HQ
            if (row.District && row.District !== '') {
                row.Unit = 'L&O';
                updatedCount++;
            }
        }
    }
});

console.log(`Cleaned up ${updatedCount} redundant Range values from the Unit column.`);

const headers = ['agid', 'Section', 'Unit', 'Range', 'District', 'Sub Division', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2', 'searchBlob'];
const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h] || ''));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map(() => ({wch: 25}));
wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);

console.log('Successfully updated Excel. Refreshing sheets...');
