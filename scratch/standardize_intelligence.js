const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = 'ALL';
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

let updatedCount = 0;

data.forEach(row => {
    const isIntel = (row.Unit === 'State Intelligence' || row.Unit === 'INT' || row.Unit === 'S INT' || row.Unit === 'Statelevel');
    const isIntelRange = row.Range && (row.Range.includes('Intelligence') || row.Range.includes('INT'));
    
    if (isIntel || (isIntelRange && (row.Unit === 'Statelevel' || row.Unit === 'INT'))) {
        const name = (row.Name || '').toLowerCase();
        
        // 1. Districts (Regional Divisions)
        if (name.includes('division') || name.includes('cell') || name.includes('mysuru') || name.includes('belagavi') || name.includes('mangaluru') || name.includes('kalaburagi')) {
            // But if it's Admin or Chief, it's still HQ
            if (name.includes('admin') || name.includes('chief')) {
                row.Unit = 'HQ';
            } else {
                row.Unit = 'Districts';
            }
            updatedCount++;
        }
        // 2. HQ (Top Leadership and Admin)
        else {
            row.Unit = 'HQ';
            updatedCount++;
        }
    }
});

console.log(`Updated ${updatedCount} State Intelligence records to distinguish between 'HQ' and 'Districts'.`);

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
