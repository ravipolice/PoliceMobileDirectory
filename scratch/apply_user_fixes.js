const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];

const data = xlsx.utils.sheet_to_json(sheet);

let cidHqFixed = 0;
let controlRoomRenamed = 0;

data.forEach(row => {
    // 1. Fix CID HQ -> Bengaluru City
    if (row.Section === 'CID' || (row.Unit && row.Unit.includes('CID'))) {
        // If it's general CID HQ or mentions Bengaluru
        if (!row.District || row.District === 'UNKNOWN' || row.District === 'State Level' || row.District === '') {
             row.District = 'Bengaluru City';
             cidHqFixed++;
        }
    }

    // 2. Rename Control Room to C/Room (in Name, Rank, or Station)
    const fieldsToRename = ['Name', 'Rank', 'Station'];
    fieldsToRename.forEach(field => {
        if (row[field] && typeof row[field] === 'string') {
            const original = row[field];
            // Case-insensitive replacement for 'Control Room'
            const regex = /Control\s+Room/gi;
            if (regex.test(original)) {
                row[field] = original.replace(regex, 'C/Room');
                controlRoomRenamed++;
            }
        }
    });
});

console.log(`Updated District for ${cidHqFixed} CID records.`);
console.log(`Renamed "Control Room" to "C/Room" in ${controlRoomRenamed} instances.`);

const headers = ['Section', 'Unit', 'Range', 'District', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2'];
const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h] || ''));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map((h, i) => ({wch: [20, 20, 20, 20, 35, 20, 25, 15, 15, 15, 15, 30, 30][i]}));

wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);
console.log(`Successfully updated ${filePath}`);
