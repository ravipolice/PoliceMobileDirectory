const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

const locationsToStrip = [
    'Bengaluru City', 'Bengaluru', 'Bangalore',
    'Mysuru City', 'Mysuru', 'Mysore',
    'Hubballi-Dharwad City', 'Hubballi-Dharwad', 'Hubballi', 'Hubli',
    'Mangaluru City', 'Mangaluru', 'Mangalore',
    'Belagavi City', 'Belagavi', 'Belgaum',
    'Kalaburagi City', 'Kalaburagi', 'Gulbarga',
    'Ballari', 'Bellary',
    'Dharwad', 'Davanagere', 'Shivamogga', 'Shimoga',
    'Tumakuru', 'Tumkur', 'Vijayapura', 'Bijapur'
];

let cleanedCount = 0;

data.forEach(row => {
    if (row.Name) {
        let original = row.Name;
        let cleaned = original;

        // Strip location from the end
        for (const loc of locationsToStrip) {
            // Match location at the end, possibly preceded by a comma and/or spaces
            const regex = new RegExp(`[,\\s]*${loc}[,\\s]*$`, 'i');
            if (regex.test(cleaned)) {
                cleaned = cleaned.replace(regex, '').trim();
                // If it ends with a comma now, remove it
                cleaned = cleaned.replace(/,$/, '').trim();
            }
        }
        
        if (cleaned !== original && cleaned.length > 2) {
            row.Name = cleaned;
            cleanedCount++;
        }
    }
});

console.log(`Cleaned ${cleanedCount} names by removing redundant location suffixes.`);

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
