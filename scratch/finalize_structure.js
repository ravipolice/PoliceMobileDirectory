const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = 'ALL';
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

let updatedCount = 0;

data.forEach(row => {
    // 1. Move 'HQ' units to 'Statelevel'
    if (row.Unit === 'HQ' || row.Section === 'State Police HQ' || row.Section === 'State Police Headquarters') {
        if (row.Unit === 'HQ' || !row.Unit) {
            row.Unit = 'Statelevel';
            updatedCount++;
        }
    }
    
    // 2. Clear the Section column (making it ready for the user's ministerial data)
    // We'll keep the column header but empty the values if they are the old 'Ranges', 'KSRP' etc.
    const oldSections = ['Ranges', 'Commissionerate', 'KSRP', 'CID', 'ISD', 'State Intelligence', 'State Police HQ', 'Administration', 'Crime', 'ANTF', 'BMTF', 'CLM', 'DCRE', 'ESCOM', 'FSL', 'KLA', 'KPA', 'KSCA', 'KSPH & IDCL', 'KSSPB', 'SIT', 'SPORTI', 'STF', 'Training', 'Wireless'];
    if (oldSections.includes(row.Section)) {
        row.Section = ''; 
    }
});

console.log(`Updated ${updatedCount} records to 'Statelevel' unit and cleared old section categories for ministerial use.`);

const headers = ['agid', 'Section', 'Unit', 'Range', 'District', 'Sub Division', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2', 'searchBlob'];
const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h] || ''));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map(() => ({wch: 25}));
wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);
console.log(`Successfully updated ${filePath}`);
