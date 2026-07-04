const fs = require('fs');
const path = require('path');
const xlsx = require('xlsx');

const csvPath = path.join(__dirname, '..', 'KSP_Contacts_Master_Clean.csv');
const csvContent = fs.readFileSync(csvPath, 'utf8');
const csvData = xlsx.utils.sheet_to_json(xlsx.read(csvContent, { type: 'string' }).Sheets.Sheet1);

// Rank Mapping
const rankMapping = {
    'DIRECTOR GENERAL & INSPECTOR GENERAL OF POLICE': 'DG & IGP',
    'DIRECTOR GENERAL AND INSPECTOR GENERAL OF POLICE': 'DG & IGP',
    'DG & IGP': 'DG & IGP', 'DIRECTOR GENERAL & IGP': 'DG & IGP', 'DIRECTOR GENERAL AND IGP': 'DG & IGP',
    'ADDITIONAL DIRECTOR GENERAL OF POLICE': 'ADGP', 'ADDITIONAL DGP': 'ADGP', 'ADDL DGP': 'ADGP', 'ADGP': 'ADGP',
    'DGP': 'DGP', 'DIRECTOR GENERAL OF POLICE': 'DGP',
    'INSPECTOR GENERAL OF POLICE': 'IGP', 'IGP': 'IGP',
    'DEPUTY INSPECTOR GENERAL OF POLICE': 'DIG', 'DIGP': 'DIG', 'DIG': 'DIG',
    'ADDITIONAL SUPERINTENDENT OF POLICE': 'Addl.SP', 'ADDL. SP': 'Addl.SP', 'ADDL.SP': 'Addl.SP', 'ADDL SP': 'Addl.SP',
    'SUPERINTENDENT OF POLICE': 'SP', 'SP': 'SP',
    'DEPUTY COMMISSIONER OF POLICE': 'DCP', 'DCP': 'DCP',
    'DEPUTY SUPERINTENDENT OF POLICE': 'DySP', 'DYSP': 'DySP', 'DSP': 'DySP',
    'ASSISTANT SUPERINTENDENT OF POLICE': 'ASP', 'ASP': 'ASP',
    'ASSISTANT COMMISSIONER OF POLICE': 'ACP', 'ACP': 'ACP',
    'COMMANDANT': 'CMDT', 'CMDT': 'CMDT',
    'DEPUTY COMMANDANT': 'DEPT.CMDT', 'DEPT.CMDT': 'DEPT.CMDT',
    'ASSISTANT COMMANDANT': 'ASST.CMDT', 'ASST.CMDT': 'ASST.CMDT',
    'CIRCLE POLICE INSPECTOR': 'CPI', 'CIRCLE PI': 'CPI', 'CPI': 'CPI',
    'RESERVE POLICE INSPECTOR': 'RPI', 'RESERVE PI': 'RPI', 'RPI': 'RPI',
    'WOMEN POLICE INSPECTOR': 'WPI', 'WPI': 'WPI',
    'POLICE INSPECTOR': 'PI', 'PI': 'PI',
    'POLICE SUB INSPECTOR': 'PSI', 'SUB-INSPECTOR': 'PSI', 'PSI': 'PSI',
    'ASSISTANT SUB INSPECTOR': 'ASI', 'ASI': 'ASI',
    'HEAD CONSTABLE': 'HC', 'HC': 'HC',
    'POLICE CONSTABLE': 'PC', 'PC': 'PC',
    'ADDL. IG': 'ADDL.IG', 'ADDITIONAL INSPECTOR GENERAL': 'ADDL.IG',
    'CHIEF SUPERINTENDENT': 'C.SUPT',
    'ASST. SUPERINTENDENT': 'A.SUPT',
    'ASSISTANT SUPERINTENDENT': 'A.SUPT',
    'SUPERINTENDENT OF PRISON': 'SUPT',
    'PRINCIPAL': 'PRIN',
    'AO': 'AO', 'ADMINISTRATIVE OFFICER': 'AO',
    'AAO': 'AAO', 'ASSISTANT ADMINISTRATIVE OFFICER': 'AAO',
    'DDP': 'DDP', 'DEPUTY DIRECTOR': 'DDP',
    'SS': 'SS', 'SECTION SUPERINTENDENT': 'SS',
    'SYSTEM ADMIN': 'System Admin', 'SYSTEM ADMINISTRATOR': 'System Admin',
    'S.S': 'SS',
    'FDA': 'FDA', 'SDA': 'SDA', 'STENO': 'STENO', 'TYPIST': 'TYPIST'
};

const unitMapping = {
    'Law & Order': 'L&O',
    'Criminal Investigation Department': 'CID', 'CID': 'CID',
    'State Intelligence': 'Intelligence', 'Intelligence': 'Intelligence', 'INT.': 'Intelligence',
    'Karnataka State Reserve Police': 'KSRP', 'KSRP': 'KSRP',
    'Internal Security Division': 'ISD', 'ISD': 'ISD',
    'Directorate of Civil Rights Enforcement': 'DCRE', 'DCRE': 'DCRE',
    'Directorate of Forensic Science Laboratory': 'FSL', 'FSL': 'FSL',
    'Finger Print Bureau': 'FPB', 'FPB': 'FPB',
    'Police Training': 'Training', 'Training': 'Training',
    'Karnataka Railways': 'Railway', 'Railway': 'Railway',
    'Coastal Security': 'CSP', 'CSP': 'CSP',
    'State Police Headquarters': 'Admin', 'Headquarters': 'Admin', 'Administration': 'Admin', 'Admin': 'Admin',
    'Karnataka State Police': 'Admin', 'DGP Office': 'Admin',
    'Karnataka Lokayukta': 'Lokayukta', 'Lokayukta': 'Lokayukta',
    'Police Computer Wing': 'Computer', 'Computer': 'Computer',
    'Control Room': 'C/Room', 'C/Room': 'C/Room',
    'Communication, Logistics': 'Wireless', 'Wireless': 'Wireless', 'Motor Transport': 'Wireless',
    'Prison': 'Prison', 'Forest Cell': 'Forest', 'Excise': 'Excise', 'Health': 'Health',
    'VVIP': 'VVIP', 'Traffic': 'Traffic', 'SCRB': 'SCRB',
    'BMTF': 'BMTF', 'Recruitment': 'Recruitment', 'IRB': 'IRB', 'KSPH': 'KSPH', 'KPA': 'Training', 'SPORTI': 'Training',
    'Sports': 'Sports', 'SIT': 'SIT', 'STF': 'STF', 'SAF': 'SAF',
    'Range': 'L&O', 'Commissionerate': 'L&O', 'City': 'L&O', 'Hubballi': 'L&O',
    'Home': 'Home', 'Deputation': 'Others', 'Retired': 'Retired',
    'CAR': 'CAR', 'DAR': 'DAR', 'CCB': 'CCB', 'CCRB': 'CCRB', 'DCRB': 'DCRB', 'CEN': 'CEN', 'Cyber': 'CEN'
};

const cities = ["Bengaluru City", "Belagavi City", "Hubballi Dharwad City", "Kalaburagi City", "Mangaluru City", "Mysuru City"];

function getRank(rawRank) {
    if (!rawRank) return "";
    let r = String(rawRank).toUpperCase().trim();
    let isRetd = r.includes("RETD") || r.includes("RETIRED");
    const sortedKeys = Object.keys(rankMapping).sort((a, b) => b.length - a.length);
    for (const key of sortedKeys) {
        if (r.startsWith(key)) return (isRetd ? "RETD. " : "") + rankMapping[key];
    }
    for (const key of sortedKeys) {
        if (r.includes(key)) {
            const matchIndex = r.indexOf(key);
            const contextBefore = r.substring(0, matchIndex);
            if (!contextBefore.includes("TO ") && !contextBefore.includes("OFFICE") && !contextBefore.includes("OF ")) {
                return (isRetd ? "RETD. " : "") + rankMapping[key];
            }
        }
    }
    return "";
}

function shortenNames(name) {
    if (!name) return "";
    let n = String(name).trim().replace(/[â\x80-\x9F]/g, "");
    const sortedKeys = Object.keys(rankMapping).sort((a, b) => b.length - a.length);
    for (const key of sortedKeys) {
        const regex = new RegExp(`\\b${key.replace('.', '\\.')}\\b`, 'gi');
        n = n.replace(regex, rankMapping[key]);
    }
    return n;
}

function getUnitShortCode(rawUnit) {
    if (!rawUnit) return "Others";
    let u = String(rawUnit).trim();
    u = u.replace(/^(ADGP|IGP|DIG|SP|DySP|PI|PSI|PA|AAO|SS|FDA|SDA|STENO|TYPIST)[,\s]+/i, "").trim();
    if (u.includes("L&O") || u.includes("L & O") || u.includes("Law & Order") || u.includes("Law and Order")) return "L&O";
    const sortedKeys = Object.keys(unitMapping).sort((a, b) => b.length - a.length);
    for (const key of sortedKeys) {
        if (u.toUpperCase().includes(key.toUpperCase())) return unitMapping[key];
    }
    return u;
}

const processedAAO = [];
csvData.forEach((r, idx) => {
    const rawCol1 = String(r.Section || "").trim();
    const rawCol2 = String(r.Unit || "").trim();
    const name = shortenNames(r.Designation || "");
    const rank = getRank(r.Designation || "");
    
    if (rank === "AAO" || name.includes("AAO")) {
        let unit = getUnitShortCode(rawCol2 || rawCol1);
        let range = "";
        let district = "";
        let section = rawCol1;

        if (rawCol1 === "Commissionerates" || cities.includes(rawCol2)) {
            unit = "L&O";
            range = rawCol2;
            district = rawCol2;
        } else if (rawCol1.includes("Range")) {
            unit = "L&O";
            range = rawCol1;
            district = rawCol2;
        }

        processedAAO.push({
            line: idx + 2,
            rawCol1,
            rawCol2,
            name,
            rank,
            unit,
            range,
            district,
            section
        });
    }
});

console.log(JSON.stringify(processedAAO, null, 2));
