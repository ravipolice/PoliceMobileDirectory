const xlsx = require('xlsx');
const ExcelJS = require('exceljs');
const path = require('path');
const fs = require('fs');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);

const rankMapping = {
    'DG & IGP': 'DG & IGP',
    'DIRECTOR GENERAL & IGP': 'DG & IGP',
    'DIRECTOR GENERAL AND IGP': 'DG & IGP',
    'DGP': 'DGP',
    'DIRECTOR GENERAL OF POLICE': 'DGP',
    'ADGP': 'ADGP',
    'ADDITIONAL DIRECTOR GENERAL OF POLICE': 'ADGP',
    'ADDITIONAL DGP': 'ADGP',
    'ADDL DGP': 'ADGP',
    'IGP': 'IGP',
    'INSPECTOR GENERAL OF POLICE': 'IGP',
    'DIGP': 'DIG',
    'DEPUTY INSPECTOR GENERAL OF POLICE': 'DIG',
    'DIG': 'DIG',
    'SP': 'SP',
    'SUPERINTENDENT OF POLICE': 'SP',
    'DCP': 'DCP',
    'DEPUTY COMMISSIONER OF POLICE': 'DCP',
    'ADDL. SP': 'Addl.SP',
    'ADDL.SP': 'Addl.SP',
    'ADDL SP': 'Addl.SP',
    'ADDITIONAL SUPERINTENDENT OF POLICE': 'Addl.SP',
    'DYSP': 'DySP',
    'DEPUTY SUPERINTENDENT OF POLICE': 'DySP',
    'DSP': 'DySP',
    'ASP': 'ASP',
    'ASSISTANT SUPERINTENDENT OF POLICE': 'ASP',
    'ACP': 'ACP',
    'ASSISTANT COMMISSIONER OF POLICE': 'ACP',
    'CMDT': 'CMDT',
    'COMMANDANT': 'CMDT',
    'DEPT.CMDT': 'DEPT.CMDT',
    'DEPT CMDT': 'DEPT.CMDT',
    'DEPUTY COMMANDANT': 'DEPT.CMDT',
    'ASST.CMDT': 'ASST.CMDT',
    'ASST CMDT': 'ASST.CMDT',
    'ASSISTANT COMMANDANT': 'ASST.CMDT',
    'CPI': 'CPI',
    'CIRCLE PI': 'CPI',
    'CIRCLE POLICE INSPECTOR': 'CPI',
    'RPI': 'RPI',
    'RESERVE PI': 'RPI',
    'RESERVE POLICE INSPECTOR': 'RPI',
    'WPI': 'WPI',
    'WOMEN POLICE INSPECTOR': 'WPI',
    'PI': 'PI',
    'POLICE INSPECTOR': 'PI',
    'PSI': 'PSI',
    'POLICE SUB INSPECTOR': 'PSI',
    'SUB-INSPECTOR': 'PSI',
    'ASI': 'ASI',
    'ASSISTANT SUB INSPECTOR': 'ASI',
    'HC': 'HC',
    'HEAD CONSTABLE': 'HC',
    'PC': 'PC',
    'POLICE CONSTABLE': 'PC',
    'AO': 'AO',
    'ADMINISTRATIVE OFFICER': 'AO',
    'AAO': 'AAO',
    'ASSISTANT ADMINISTRATIVE OFFICER': 'AAO'
};

const officialRanks = [
    "DG & IGP", "DG", "DGP", "ADGP", "IGP", "DIG", "DCP", "CMDT", "DEPT.CMDT", "ASST.CMDT", "SP", "Addl.SP", "ASP", "DySP", "ACP", 
    "PI", "PIW", "RPI", "S.RPI", "CPI", "WPI",
    "RSI", "S.RSI", "PSI", "PSIW", "WPSI",
    "ASI", "WASI", "ARSI", "ASIW", "S.ARSI",
    "CHC", "AHC", "S.RHC", "WHC", "HCW", "HC",
    "CPC", "APC", "S.RPC", "WPC", "PCW", "PC",
    "FDA", "SDA", "SS", "STENO", "TYPIST", "PA", "FOLLOWER",
    "IA", "AIO", "IO", "SIA", "CIO", "AAO", "AD", "DD", "AO"
];

const stdMapping = {
    "Bengaluru City": "080", "Bengaluru Urban": "080", "Bengaluru Dist": "080", "Bengaluru Rural": "080", "Ramanagara": "080", "Admin": "080", "CID": "080", "Intelligence": "080", "KSRP": "080", "ISD": "080", "Railway": "080", "Prison": "080", "Home Guard": "080",
    "Mysuru City": "0821", "Mysuru Dist": "0821", "Mysuru": "0821", "Chamarajanagar": "08226", "Mandya": "08232", "Hassan": "08172", "Kodagu": "08272",
    "Hubballi Dharwad City": "0836", "Hubballi-Dharwad": "0836", "Dharwad": "0836", "Gadag": "08372", "Haveri": "08375",
    "Mangaluru City": "0824", "Dakshina Kannada": "0824", "Mangaluru": "0824", "Udupi": "0820", "Chikkamagaluru": "08262",
    "Belagavi City": "0831", "Belagavi Dist": "0831", "Belagavi": "0831", "Bagalkote": "08354", "Vijayapura": "08352",
    "Kalaburagi City": "08472", "Kalaburagi": "08472", "Bidar": "08482", "Yadgir": "08473",
    "Ballari": "08392", "Vijayanagara": "08394", "Raichur": "08532", "Koppal": "08539",
    "Davanagere": "08192", "Chitradurga": "08194", "Shivamogga": "08182",
    "Tumakuru": "0816", "Kolar": "08152", "Chikkaballapura": "08156"
};

const sortedOfficial = [...officialRanks].sort((a, b) => b.length - a.length);
const emailRegex = /[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/;

function cleanRank(rankStr) {
    if (!rankStr) return "";
    let r = String(rankStr).toUpperCase().trim();
    let isRetd = r.startsWith("RETD.") || r.startsWith("RETIRED");
    if (isRetd) r = r.replace(/^RETD\.\s*|^RETIRED\s*/, "").trim();

    let found = "";
    const sortedKeys = Object.keys(rankMapping).sort((a, b) => b.length - a.length);
    for (const key of sortedKeys) {
        const escapedKey = key.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
        let pattern = "";
        if (/^[A-Za-z0-9]/.test(key)) pattern += "\\b";
        pattern += escapedKey;
        if (/[A-Za-z0-9]$/.test(key)) pattern += "\\b";
        
        const regex = new RegExp(pattern, 'i');
        if (regex.test(r)) {
            found = rankMapping[key];
            break;
        }
    }

    if (!found) {
        for (const off of sortedOfficial) {
            const regex = new RegExp(`\\b${off.replace('.', '\\.')}\\b`, 'i');
            if (regex.test(r)) {
                found = off;
                break;
            }
        }
    }

    if (found) return (isRetd ? "RETD. " : "") + found;
    let simple = r.split(',')[0].split('(')[0].split('-')[0].trim();
    return (isRetd ? "RETD. " : "") + simple;
}

function cleanEmail(emailStr) {
    if (!emailStr) return "";
    const match = String(emailStr).match(emailRegex);
    return match ? match[0].toLowerCase() : "";
}

function cleanPhone(val) {
    if (!val) return "";
    return String(val).replace(/\s/g, '').replace(/—/g, '');
}

async function finalize() {
    const workbook = new ExcelJS.Workbook();
    const headers = ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"];

    for (const sheetName of wbSource.SheetNames) {
        const data = xlsx.utils.sheet_to_json(wbSource.Sheets[sheetName]);
        const worksheet = workbook.addWorksheet(sheetName);

        worksheet.columns = headers.map(h => ({ header: h, key: h, width: 20 }));

        data.forEach(row => {
            // Normalize Rank
            row.Rank = cleanRank(row.Rank);
            
            // Shorten ranks in Name column
            if (row.Name) {
                for (const [key, val] of Object.entries(rankMapping)) {
                    const regex = new RegExp(`\\b${key.replace('.', '\\.')}\\b`, 'gi');
                    row.Name = row.Name.replace(regex, val);
                }
            }
            
            row.email1 = cleanEmail(row.email1);
            row.email2 = cleanEmail(row.email2);

            // STD Code Logic
            const distKey = String(row.District || row.UNIT || "").trim();
            let std = "";
            for (const key of Object.keys(stdMapping)) {
                if (distKey.includes(key)) {
                    std = stdMapping[key];
                    break;
                }
            }

            const applyStd = (num) => {
                if (!num) return "";
                let n = cleanPhone(num);
                
                // If it's just an STD code or trash (less than 5 digits total), clear it
                if (n.replace(/-/g, '').length < 5) return ""; 

                if (n.length >= 6 && n.length <= 8 && std && !n.startsWith('0')) {
                    return `${std}-${n}`;
                }
                // If it already has an STD but maybe wrong? User said "update all"
                if (n.startsWith('0') && n.includes('-')) {
                    let [oldStd, rest] = n.split('-');
                    if (!rest || rest.length < 5) return ""; // Clear if no actual number
                    if (std && oldStd !== std && rest.length >= 6) {
                        return `${std}-${rest}`;
                    }
                }
                return n;
            };

            row.office1 = applyStd(row.office1);
            row['office 2'] = applyStd(row['office 2']);

            if (row['mobile 1']) row['mobile 1'] = String(row['mobile 1']).replace(/\s/g, '');
            if (row['mobile 2']) row['mobile 2'] = String(row['mobile 2']).replace(/\s/g, '');

            worksheet.addRow(row);
        });

        // Formatting
        worksheet.getRow(1).font = { bold: true };
        worksheet.views = [{ state: 'frozen', ySplit: 1 }];
    }

    await workbook.xlsx.writeFile(masterPath);
    console.log(`Finalized directory saved to ${masterPath} (with Freeze Panes)`);

    // Generate CSV for Android App (using xlsx as it's easier for CSV from the existing wbSource-like data)
    // We'll just read from the newly saved file to be sure
    const finalWb = xlsx.readFile(masterPath);
    const masterData = xlsx.utils.sheet_to_json(finalWb.Sheets['MASTER_MERGED_FINAL']);
    const appData = masterData.map(r => {
        const blobParts = [r.Name, r.Rank, r.station, r.UNIT, r.District, r.Section, r.office1, r['office 2'], r['mobile 1'], r['mobile 2'], r.email1, r.email2]
            .filter(v => v && String(v).trim() !== '').map(v => String(v).toLowerCase());
        return {
            agid: r.agid || '',
            name: r.Name || '',
            rank: r.Rank || '',
            station: r.station || '',
            unit: r.UNIT || '',
            district: r.District || '',
            subDivision: r.Section || '',
            landline: r.office1 || '',
            landline2: r['office 2'] || '',
            mobile: r['mobile 1'] || '',
            mobile2: r['mobile 2'] || '',
            email: r.email1 || '',
            email2: r.email2 || '',
            searchBlob: [...new Set(blobParts)].join(' ')
        };
    });
    const appSheet = xlsx.utils.json_to_sheet(appData);
    const csvContent = xlsx.utils.sheet_to_csv(appSheet);
    fs.writeFileSync(path.join(__dirname, '..', 'KSP_Officers_App_FINAL.csv'), csvContent, 'utf8');
    console.log('Android App CSV saved.');
}

finalize().catch(console.error);
