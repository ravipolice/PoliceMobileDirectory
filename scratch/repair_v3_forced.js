const xlsx = require('xlsx');
const path = require('path');
const fs = require('fs');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
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

wb.SheetNames.forEach(sheetName => {
    const sheet = wb.Sheets[sheetName];
    const data = xlsx.utils.sheet_to_json(sheet);
    
    data.forEach(row => {
        let name = String(row.Name || '').trim();
        let rank = String(row.Rank || '').trim();
        let station = String(row.Station || '').trim();

        // 1. If Rank/Station are empty or just acronyms, try to extract from Name
        const isAcronym = rankMapping[rank.toUpperCase()];
        if ((!rank || isAcronym || rank === '') && name !== '') {
            const match = name.match(acronymRegex);
            if (match) {
                const baseRank = match[1].toUpperCase();
                const suffix = match[2] || '';
                const rest = match[3] || '';
                
                row.Rank = baseRank + suffix;
                row.Station = rest;
                rank = row.Rank;
                station = row.Station;
            }
        }

        // 2. Expand Rank
        if (rank) {
            let baseRankMatch = rank.match(/^([A-Z\.]+)(-\d+)?$/i);
            if (baseRankMatch) {
                let base = baseRankMatch[1].toUpperCase();
                let suffix = baseRankMatch[2] || '';
                if (rankMapping[base]) {
                    row.Rank = rankMapping[base] + suffix;
                }
            }
            
            // 3. Update Name to be the full expanded form
            if (row.Station) {
                row.Name = `${row.Rank}, ${row.Station}`;
            } else {
                row.Name = row.Rank;
            }
        }
    });

    const newSheet = xlsx.utils.json_to_sheet(data);
    wb.Sheets[sheetName] = newSheet;
});

xlsx.writeFile(wb, filePath);

console.log('RE-REPAIRED V3 with forced expansion in both Rank and Name columns.');
