const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

const ministerialRanks = [
    { rank: 'AO', pattern: /^(AO|Admin\s*Officer)/i },
    { rank: 'AAO', pattern: /^(AAO|Asst\.\s*Admin\s*Officer)/i },
    { rank: 'CAO', pattern: /^(CAO|Chief\s*Admin\s*Officer)/i },
    { rank: 'PA to', pattern: /^(PA\s*to|PATO|P\.A\.\s*to)/i },
    { rank: 'FDA', pattern: /^(FDA|First\s*Division\s*Assistant)/i },
    { rank: 'SDA', pattern: /^(SDA|Second\s*Division\s*Assistant)/i },
    { rank: 'Suptd.', pattern: /^(Superintendent|Suptd\.)/i }
];

let updatedCount = 0;

data.forEach(row => {
    if (row.Name) {
        for (const r of ministerialRanks) {
            if (r.pattern.test(row.Name)) {
                // If Rank is empty or incorrect, fill it
                if (!row.Rank || row.Rank === '' || row.Rank === 'PA to') {
                    row.Rank = r.rank;
                    updatedCount++;
                }
                
                // Shorten in Name too
                const regex = new RegExp(`^${r.pattern.source}`, 'i');
                row.Name = row.Name.replace(regex, r.rank);
                break;
            }
        }
    }
});

console.log(`Updated ${updatedCount} ministerial/administrative rank entries (AO, PA, AAO, etc.).`);

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
