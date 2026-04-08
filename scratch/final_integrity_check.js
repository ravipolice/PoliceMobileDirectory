const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = 'ALL';
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

console.log(`Starting integrity check on ${data.length} records...`);

let emailFixed = 0;
let phoneFixed = 0;

data.forEach((row, index) => {
    // 1. Regenerate AGID to be perfectly sequential
    row.agid = 'KSP' + (index + 1).toString().padStart(4, '0');

    // 2. Standardize Mobile Numbers
    ['Mobile 1', 'Mobile 2', 'Office 1', 'Office 2'].forEach(field => {
        if (row[field]) {
            let num = String(row[field]).trim();
            // Remove spaces and extra dashes, keep initial + if present
            const original = num;
            num = num.replace(/\s+/g, '');
            if (num !== original) {
                row[field] = num;
                phoneFixed++;
            }
        }
    });

    // 3. Standardize Emails
    ['Email', 'Email2'].forEach(field => {
        if (row[field]) {
            let email = String(row[field]).trim().toLowerCase();
            if (row[field] !== email) {
                row[field] = email;
                emailFixed++;
            }
        }
    });

    // 4. Regenerate Search Blob
    const blobParts = [
        row.Name, row.Rank, row.Station, row.Unit, row.District, 
        row['Sub Division'], row['Mobile 1'], row.Email, row['Office 1']
    ].filter(v => v && String(v).trim() !== '').map(v => String(v).toLowerCase());
    
    row.searchBlob = [...new Set(blobParts)].join(' ');
});

console.log(`- Standardized ${phoneFixed} phone numbers.`);
console.log(`- Standardized ${emailFixed} email addresses.`);
console.log(`- Re-indexed all ${data.length} AGIDs.`);
console.log(`- Updated all search blobs.`);

const headers = ['agid', 'Section', 'Unit', 'Range', 'District', 'Sub Division', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2', 'searchBlob'];
const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h] || ''));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map(() => ({wch: 25}));
wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);

console.log('Integrity check complete. Now refreshing workbook tabs and App CSV...');
