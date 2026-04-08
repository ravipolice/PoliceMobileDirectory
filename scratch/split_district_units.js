const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

const districtUnitKeywords = {
    'DCRB': 'DCRB',
    'COMPUTER': 'Computer Wing',
    'FPB': 'FPB',
    'DSB': 'DSB',
    'SMMC': 'SMMC',
    'C/Room': 'C/Room'
};

const keys = Object.keys(districtUnitKeywords).sort((a, b) => b.length - a.length);

let updatedCount = 0;

data.forEach(row => {
    // Only target district/range level records (Ranges and Commissionerates)
    if (row.Section === 'Ranges' || row.Section === 'Commissionerate') {
        if (row.Name) {
            for (const key of keys) {
                const regex = new RegExp(`\\b${key.replace('/', '\\/')}\\b`, 'i');
                if (regex.test(row.Name)) {
                    const newUnit = districtUnitKeywords[key];
                    if (row.Unit !== newUnit) {
                        row.Unit = newUnit;
                        updatedCount++;
                    }
                    break;
                }
            }
        }
    }
});

console.log(`Reclassified ${updatedCount} district records into specific units (DCRB, DSB, C/Room, etc.).`);

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
