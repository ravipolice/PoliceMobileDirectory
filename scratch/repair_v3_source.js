const xlsx = require('xlsx');
const path = require('path');
const fs = require('fs');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const backupPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3_Backup.xlsx');

// Create a backup first
fs.copyFileSync(filePath, backupPath);

const wb = xlsx.readFile(filePath);

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

const acronyms = Object.keys(rankMapping).sort((a, b) => b.length - a.length);
const acronymRegex = new RegExp(`^(${acronyms.join('|')})(-\\d+)?(?:\\s+|,\\s+)(.*)$`, 'i');

let totalExtracted = 0;
let totalExpanded = 0;

wb.SheetNames.forEach(sheetName => {
    const sheet = wb.Sheets[sheetName];
    const data = xlsx.utils.sheet_to_json(sheet);
    let sheetModified = true; // Set to true to apply view settings to all sheets

    data.forEach(row => {
        let name = String(row.Name || '').trim();
        let rank = String(row.Rank || '').trim();
        let station = String(row.Station || '').trim();

        if (!rank && name) {
            const match = name.match(acronymRegex);
            if (match) {
                const baseRank = match[1].toUpperCase();
                const suffix = match[2] || '';
                const rest = match[3] || '';
                
                row.Rank = baseRank + suffix;
                row.Station = rest;
                rank = row.Rank;
                station = row.Station;
                totalExtracted++;
            }
        }

        if (rank) {
            let originalRank = rank;
            let baseRankMatch = rank.match(/^([A-Z\.]+)(-\d+)?$/i);
            if (baseRankMatch) {
                let base = baseRankMatch[1].toUpperCase();
                let suffix = baseRankMatch[2] || '';
                if (rankMapping[base]) {
                    rank = rankMapping[base] + suffix;
                }
            }

            if (rank !== originalRank) {
                row.Rank = rank;
                totalExpanded++;
            }

            if (row.Rank && row.Station) {
                row.Name = `${row.Rank}, ${row.Station}`;
            }
        }
    });

    const newSheet = xlsx.utils.json_to_sheet(data);
    
    // ✅ Set freezing of top row
    newSheet['!views'] = [
        { state: 'frozen', ySplit: 1, xSplit: 0, topLeftCell: 'A2', activePane: 'bottomLeft' }
    ];

    // Set some basic column widths for better visibility
    newSheet['!cols'] = [
        { wch: 10 }, // agid
        { wch: 25 }, // Unit
        { wch: 20 }, // Range
        { wch: 20 }, // District
        { wch: 20 }, // Sub Division
        { wch: 20 }, // Section
        { wch: 40 }, // Name
        { wch: 30 }, // Rank
        { wch: 30 }, // Station
        { wch: 15 }, // Office 1
        { wch: 15 }, // Office 2
        { wch: 15 }, // Mobile 1
        { wch: 15 }, // Mobile 2
        { wch: 25 }, // Email 1
        { wch: 25 }  // Email 2
    ];

    wb.Sheets[sheetName] = newSheet;
});

xlsx.writeFile(wb, filePath);

console.log(`Updated V3 Excel File with Freeze Panes!`);
console.log(`Extracted Ranks: ${totalExtracted}, Expanded Ranks: ${totalExpanded}`);
