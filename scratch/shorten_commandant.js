const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

const replacements = [
    { regex: /Assistant Commandant/gi, replacement: 'Asst. Commdt.' },
    { regex: /Deputy Commandant/gi, replacement: 'Dy. Commdt.' },
    { regex: /Commandant/gi, replacement: 'Commdt.' }
];

let replacedCount = 0;

data.forEach(row => {
    const fields = ['Name', 'Rank', 'Station'];
    fields.forEach(f => {
        if (row[f] && typeof row[f] === 'string') {
            let original = row[f];
            let modified = original;
            
            replacements.forEach(r => {
                if (r.regex.test(modified)) {
                    modified = modified.replace(r.regex, r.replacement);
                }
            });
            
            if (modified !== original) {
                row[f] = modified;
                replacedCount++;
            }
        }
    });
});

console.log(`Updated ${replacedCount} instances of Commandant roles to short forms.`);

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
