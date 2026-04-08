const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

const rankMap = {
    'Director General & Inspector General of Police': 'DG & IGP',
    'Additional Director General': 'ADGP',
    'Deputy Inspector General': 'DIGP',
    'Commissioner of Police': 'CP',
    'Deputy Commissioner of Police': 'DCP',
    'Assistant Commissioner of Police': 'ACP',
    'Police Inspector': 'PI',
    'Police Sub-Inspector': 'PSI',
    'Assistant Superintendent of Police': 'ASP',
    'Superintendent of Police': 'SP',
    'Deputy Superintendent of Police': 'DySP',
    'DSP': 'DySP',
    'Additional Superintendent of Police': 'Addl. SP',
    'Addl. SP-1': 'Addl. SP',
    'Addl. SP-2': 'Addl. SP',
    'Addl. SP': 'Addl. SP',
    'Circle Police Inspector': 'CPI',
    'Assistant Police Inspector': 'API',
    'Assistant Sub-Inspector': 'ASI',
    'Head Constable': 'HC',
    'Police Constable': 'PC',
    'ACP(W)': 'ACP (W)',
    'DSP(W)': 'DySP (W)',
    'DSP (W)': 'DySP (W)',
    'PI(W)': 'PI (W)',
    'PI(W)-1': 'PI (W)',
    'PI(W)-2': 'PI (W)',
    'SP(W)': 'SP (W)',
    'P.A. to': 'PA to',
    'PA TO': 'PA to'
};

let rankUpdated = 0;

data.forEach(row => {
    // 1. Standardize the Rank column
    if (row.Rank) {
        let r = row.Rank.trim();
        
        // Handle Retd roles - simplify them
        if (r.includes('(Retd.)')) {
            const match = r.match(/^([A-Z\s&]+)/i);
            if (match) {
                const baseRank = match[1].trim();
                const shortRank = rankMap[baseRank] || baseRank;
                r = `${shortRank} (Retd.)`;
            }
        } else if (rankMap[r]) {
            r = rankMap[r];
        }
        
        if (row.Rank !== r) {
            row.Rank = r;
            rankUpdated++;
        }
    }

    // 2. Also check Name field for these long ranks and shorten them
    if (row.Name) {
        let n = row.Name;
        for (const [long, short] of Object.entries(rankMap)) {
            const regex = new RegExp(`^${long}\\b`, 'i');
            if (regex.test(n)) {
                n = n.replace(regex, short);
            }
        }
        // Handle Retd in Name too
        if (n.includes('(Retd.)')) {
             n = n.replace(/\(Hon'ble.*?\)\s*/gi, '');
        }
        row.Name = n;
    }
});

console.log(`Standardized ${rankUpdated} rank entries.`);

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
