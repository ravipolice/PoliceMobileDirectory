const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = 'ALL';
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

let updatedCount = 0;

data.forEach(row => {
    const range = row.Range || '';
    const unit = row.Unit || '';
    const name = row.Name || '';
    const station = row.Station || '';
    
    // Check for redundancy (Unit repeats the Range name)
    if (unit !== '' && (unit === range || unit.includes(range) || range.includes(unit))) {
        
        // 1. Commissionerate Headquarters
        if (name === 'CP' || name.includes('Commissioner') || name.includes('Addl. CP') || name.includes('DCP Admin')) {
            row.Unit = 'Commr. HQ';
            updatedCount++;
        }
        // 2. Traffic
        else if (name.includes('Traffic') || name.includes('Tr.')) {
            row.Unit = 'Traffic';
            updatedCount++;
        }
        // 3. Crime
        else if (name.includes('Crime') || name.includes('CCB')) {
            row.Unit = 'Crime';
            updatedCount++;
        }
        // 4. Law & Order (PS)
        else if (name.includes('PS') || station.includes('PS') || name.includes('Circle') || name.includes('CPI')) {
            row.Unit = 'L&O';
            updatedCount++;
        }
        // 5. General Fallback
        else {
            // For Ranges, use Range HQ or L&O
            if (range.includes('Range')) {
                row.Unit = (name.includes('IGP') || name.includes('Range Office')) ? 'Range HQ' : 'L&O';
                updatedCount++;
            }
            // For Commissionerates, use Commr. HQ or L&O
            else {
                row.Unit = 'L&O';
                updatedCount++;
            }
        }
    }
});

console.log(`Deep cleaned ${updatedCount} redundant Unit values (Commissionerates and Ranges).`);

const headers = ['agid', 'Section', 'Unit', 'Range', 'District', 'Sub Division', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2', 'searchBlob'];
const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h] || ''));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map(() => ({wch: 25}));
wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);

console.log('Successfully updated Excel. Refreshing all 100+ sheets...');
