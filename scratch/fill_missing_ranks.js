const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

const rankPatterns = [
    { rank: 'DG & IGP', pattern: /^(DG\s*&\s*IGP|Director General)/i },
    { rank: 'ADGP', pattern: /^(ADGP|Additional Director General)/i },
    { rank: 'IGP', pattern: /^(IGP|Inspector General)/i },
    { rank: 'DIGP', pattern: /^(DIGP|Deputy Inspector General)/i },
    { rank: 'CP', pattern: /^CP\b/i },
    { rank: 'Addl. CP', pattern: /^Addl\.\s*CP/i },
    { rank: 'DCP', pattern: /^DCP\b/i },
    { rank: 'ACP', pattern: /^ACP\b/i },
    { rank: 'SP', pattern: /^SP\b/i },
    { rank: 'ASP', pattern: /^ASP\b/i },
    { rank: 'DySP', pattern: /^(DySP|DSP)/i },
    { rank: 'PI', pattern: /^PI\b/i },
    { rank: 'PSI', pattern: /^PSI\b/i },
    { rank: 'ASI', pattern: /^ASI\b/i },
    { rank: 'HC', pattern: /^HC\b/i },
    { rank: 'PC', pattern: /^PC\b/i },
    { rank: 'CPI', pattern: /^CPI\b/i },
    { rank: 'Asst. Commdt.', pattern: /^Asst\.\s*Commdt\./i },
    { rank: 'Dy. Commdt.', pattern: /^Dy\.\s*Commdt\./i },
    { rank: 'Commdt.', pattern: /^Commdt\./i },
    { rank: 'AAO', pattern: /^AAO\b/i },
    { rank: 'AO', pattern: /^AO\b/i },
    { rank: 'CAO', pattern: /^CAO\b/i },
    { rank: 'FAO', pattern: /^FAO\b/i },
    { rank: 'APRO', pattern: /^APRO\b/i },
    { rank: 'Legal Advisor', pattern: /^Legal Advisor/i },
    { rank: 'PA to', pattern: /^(PA to|PATO|P\.A\. to)/i }
];

let filledCount = 0;

data.forEach(row => {
    if (!row.Rank || row.Rank === '') {
        if (row.Name) {
            for (const p of rankPatterns) {
                if (p.pattern.test(row.Name)) {
                    row.Rank = p.rank;
                    filledCount++;
                    break;
                }
            }
        }
    }
});

console.log(`Filled ${filledCount} missing ranks by extracting them from the Name field.`);

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
