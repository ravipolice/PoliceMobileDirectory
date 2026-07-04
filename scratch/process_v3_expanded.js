const xlsx = require('xlsx');
const path = require('path');
const fs = require('fs');

const inputPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(inputPath);

const rankMapping = {
    'DGP': 'Director General of Police',
    'ADGP': 'Additional Director General of Police',
    'IGP': 'Inspector General of Police',
    'DIGP': 'Deputy Inspector General of Police',
    'DIG': 'Deputy Inspector General',
    'SP': 'Superintendent of Police',
    'ASP': 'Assistant Superintendent of Police',
    'DYSP': 'Deputy Superintendent of Police',
    'DSP': 'Deputy Superintendent of Police',
    'DCP': 'Deputy Commissioner of Police',
    'ACP': 'Assistant Commissioner of Police',
    'PI': 'Police Inspector',
    'PSI': 'Police Sub-Inspector',
    'CPI': 'Circle Police Inspector',
    'RPI': 'Reserve Police Inspector',
    'RPSI': 'Reserve Police Sub-Inspector',
    'CMDT': 'Commandant',
    'ASST.CMDT': 'Assistant Commandant',
    'DEPT.CMDT': 'Deputy Commandant',
    'AAO': 'Assistant Administrative Officer',
    'AO': 'Administrative Officer',
    'CAO': 'Chief Administrative Officer',
    'PA': 'Personal Assistant',
    'SS': 'Section Superintendent',
    'FDA': 'First Division Assistant',
    'SDA': 'Second Division Assistant',
    'CP': 'Commissioner of Police'
};

// List of acronyms for extraction from Name
const acronyms = Object.keys(rankMapping).sort((a, b) => b.length - a.length); // Longest first for greedy match
const acronymRegex = new RegExp(`^(${acronyms.join('|')})(-\\d+)?(?:\\s+|,\\s+)(.*)$`, 'i');

const regexMappings = [
    { regex: /^Addl\.\s*SP(-\d+)?$/i, replacement: 'Additional Superintendent of Police$1' },
    { regex: /^Addl\.\s*DCP$/i, replacement: 'Additional Deputy Commissioner of Police' },
    { regex: /^Addl\.\s*ACP$/i, replacement: 'Additional Assistant Commissioner of Police' },
    { regex: /^Addl\.\s*DG\s*&\s*IGP$/i, replacement: 'Additional Director General & Inspector General of Police' },
    { regex: /^DG\s*&\s*IGP$/i, replacement: 'Director General & Inspector General of Police' },
    { regex: /^PI\s+FPB$/i, replacement: 'Police Inspector, Finger Print Bureau' },
    { regex: /^PSI\s+FPB$/i, replacement: 'Police Sub-Inspector, Finger Print Bureau' },
    { regex: /^PI\s+Cyber\s+Crime\s+PS$/i, replacement: 'Police Inspector, Cyber Crime PS' },
    { regex: /^ACP\s+Cyber\s+Crime\s+PS$/i, replacement: 'Assistant Commissioner of Police, Cyber Crime PS' },
    { regex: /^PI\s+Women\s+PS$/i, replacement: 'Police Inspector, Women PS' },
];

let allData = [];
let extractionCount = 0;
let rankUpdates = 0;
let nameUpdates = 0;

const sheet = wb.Sheets['MASTER_MERGED_FINAL'];
if (sheet) {
    const data = xlsx.utils.sheet_to_json(sheet);
    
    data.forEach(row => {
        let name = String(row.Name || '').trim();
        let rank = String(row.Rank || '').trim();
        let station = String(row.Station || '').trim();

        // 1. Extract Rank and Station from Name if Rank is empty
        if ((!rank || rank === '') && name !== '') {
            const match = name.match(acronymRegex);
            if (match) {
                const baseRank = match[1].toUpperCase();
                const suffix = match[2] || '';
                const rest = match[3] || '';
                
                row.Rank = baseRank + suffix;
                row.Station = rest;
                rank = row.Rank;
                station = row.Station;
                extractionCount++;
            }
        }

        let originalRank = rank;

        // 2. Expand Rank from exact mapping (handling suffixes)
        let baseRankMatch = rank.match(/^([A-Z\.]+)(-\d+)?$/i);
        if (baseRankMatch) {
            let base = baseRankMatch[1].toUpperCase();
            let suffix = baseRankMatch[2] || '';
            if (rankMapping[base]) {
                rank = rankMapping[base] + suffix;
            }
        }

        // 3. Expand Rank from regex
        for (let m of regexMappings) {
            if (m.regex.test(rank)) {
                rank = rank.replace(m.regex, m.replacement);
                break;
            }
        }

        if (rank !== originalRank) {
            row.Rank = rank;
            rankUpdates++;
        }

        // 4. Standardize Name: "Expanded Rank, Station"
        // If we extracted or if Rank was updated, let's make Name cleaner
        if (row.Rank && row.Station) {
            row.Name = `${row.Rank}, ${row.Station}`;
            nameUpdates++;
        }
    });

    allData = allData.concat(data);
}

console.log(`Extracted Rank/Station from Name for ${extractionCount} rows.`);
console.log(`Updated ${rankUpdates} Ranks to full forms.`);
console.log(`Standardized ${nameUpdates} Names.`);

const appData = allData.map((r, index) => {
    const blobParts = [
        r.Name, r.Rank, r.Station, r.UNIT || r.Unit, r.District, r['Sub Division'], r.Section, 
        r.office1 || r['Office 1'], r['mobile 1'] || r['Mobile 1'], r.email1 || r['Email 1']
    ].filter(v => v && String(v).trim() !== '').map(v => String(v).toLowerCase());
    
    return {
        agid: r.agid || `KSP${String(index + 1).padStart(4, '0')}`,
        name: r.Name || '',
        rank: r.Rank || '',
        station: r.station || r.Station || '',
        unit: r.UNIT || r.Unit || '',
        district: r.District || '',
        subDivision: r['Sub Division'] || '',
        landline: r.office1 || r['Office 1'] || '',
        mobile: r['mobile 1'] || r['Mobile 1'] || '',
        email: r.email1 || r['Email 1'] || '',
        searchBlob: [...new Set(blobParts)].join(' ')
    };
});

const wbOut = xlsx.utils.book_new();
const wsOut = xlsx.utils.json_to_sheet(allData);
xlsx.utils.book_append_sheet(wbOut, wsOut, 'Consolidated');
xlsx.writeFile(wbOut, path.join(__dirname, '..', 'KSP_Contacts_Consolidated_V3_Expanded.xlsx'));

const appSheet = xlsx.utils.json_to_sheet(appData);
const csvContent = xlsx.utils.sheet_to_csv(appSheet);
fs.writeFileSync(path.join(__dirname, '..', 'KSP_Officers_App.csv'), csvContent, 'utf8');

console.log('Successfully generated updated files.');

// Sample output for verification of the extraction
console.log('\nSample Extracted/Updated Records:');
console.table(allData.filter(r => r.Rank && r.Rank.includes('Police Sub-Inspector-1')).slice(0, 5).map(r => ({Name: r.Name, Rank: r.Rank, Station: r.Station})));
