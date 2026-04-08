const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = 'ALL';
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

const hqKeywords = [
    'antf', 'grievances', 'human rights', 'public relations', 'pro', 'legal', 
    'law', 'motor transport', 'mto', 'c/room', 'chief office', 'dgp', 'adgp', 
    'igp hq', 'digp hq', 'admin', 'l&o'
];

const preserveUnits = ['KSRP', 'CID', 'Railways', 'KSISF', 'FSL', 'KPA', 'SCRB', 'Training', 'Intelligence', 'Districts'];

let updatedCount = 0;

data.forEach(row => {
    if (row.Range === 'State Level') {
        const name = (row.Name || '').toLowerCase();
        const unit = row.Unit || '';
        
        // 1. Move Chief Office / Admin roles to HQ
        const shouldBeHQ = hqKeywords.some(k => name.includes(k) || unit.toLowerCase().includes(k));
        const isPreserved = preserveUnits.some(p => unit.includes(p));
        
        if (shouldBeHQ && !isPreserved) {
            if (row.Unit !== 'HQ') {
                row.Unit = 'HQ';
                updatedCount++;
            }
        }
        
        // 2. Fix CP Range (If it's Bengaluru City CP, it shouldn't be State Level)
        if (name === 'cp' || name.includes('commissioner of police')) {
            if (row.District === 'Bengaluru City' || row.Station.includes('Bengaluru')) {
                row.Range = 'Bengaluru City';
                updatedCount++;
            }
        }
    }
});

console.log(`Finalized ${updatedCount} state-level records. Unified Chief Office staff under 'HQ'.`);

const headers = ['agid', 'Section', 'Unit', 'Range', 'District', 'Sub Division', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2', 'searchBlob'];
const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h] || ''));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map(() => ({wch: 25}));
wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);

console.log('Successfully updated Excel. Refreshing everything one last time...');
