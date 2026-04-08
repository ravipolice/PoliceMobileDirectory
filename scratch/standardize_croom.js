const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

let replacedCount = 0;

data.forEach(row => {
    const fields = ['Section', 'Unit', 'Range', 'District', 'Name', 'Rank', 'Station'];
    fields.forEach(f => {
        if (row[f] && typeof row[f] === 'string') {
            const original = row[f];
            // Match 'Control Room' or 'Control Rooms' (case-insensitive)
            const regex = /Control Rooms?/gi;
            if (regex.test(original)) {
                row[f] = original.replace(regex, 'C/Room').trim();
                // Clean up any double spaces or odd punctuation left behind
                row[f] = row[f].replace(/\s+/g, ' ');
                replacedCount++;
            }
        }
    });
});

console.log(`Replaced "Control Room(s)" with "C/Room" in ${replacedCount} instances across all columns.`);

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
