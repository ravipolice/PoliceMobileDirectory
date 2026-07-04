const xlsx = require('xlsx');
const ExcelJS = require('exceljs');
const path = require('path');
const fs = require('fs');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);

const rankMapping = {
    'DG & IGP': 'DG & IGP', 'DIRECTOR GENERAL & IGP': 'DG & IGP', 'DIRECTOR GENERAL AND IGP': 'DG & IGP',
    'DGP': 'DGP', 'DIRECTOR GENERAL OF POLICE': 'DGP',
    'ADGP': 'ADGP', 'ADDITIONAL DIRECTOR GENERAL OF POLICE': 'ADGP',
    'IGP': 'IGP', 'INSPECTOR GENERAL OF POLICE': 'IGP',
    'DIGP': 'DIG', 'DEPUTY INSPECTOR GENERAL OF POLICE': 'DIG', 'DIG': 'DIG',
    'SP': 'SP', 'SUPERINTENDENT OF POLICE': 'SP',
    'DCP': 'DCP', 'DEPUTY COMMISSIONER OF POLICE': 'DCP',
    'ADDL. SP': 'Addl.SP', 'ADDL.SP': 'Addl.SP', 'ADDL SP': 'Addl.SP',
    'DYSP': 'DySP', 'DEPUTY SUPERINTENDENT OF POLICE': 'DySP', 'DSP': 'DySP',
    'ASP': 'ASP', 'ASSISTANT SUPERINTENDENT OF POLICE': 'ASP',
    'ACP': 'ACP', 'ASSISTANT COMMISSIONER OF POLICE': 'ACP',
    'CMDT': 'CMDT', 'COMMANDANT': 'CMDT',
    'CPI': 'CPI', 'CIRCLE PI': 'CPI', 'CIRCLE POLICE INSPECTOR': 'CPI',
    'RPI': 'RPI', 'RESERVE PI': 'RPI', 'RESERVE POLICE INSPECTOR': 'RPI',
    'PSI': 'PSI', 'POLICE SUB INSPECTOR': 'PSI'
};

const stdMapping = {
    "Bengaluru City": "080", "Bengaluru Urban": "080", "Bengaluru Dist": "080", "Bengaluru Rural": "080", "Ramanagara": "080", "Admin": "080", "CID": "080", "Intelligence": "080", "KSRP": "080", "ISD": "080", "Railway": "080", "Prison": "080", "Home Guard": "080",
    "L&O": "080", "C&TS": "080", "BMTF": "080", "DCRE": "080", "Communication": "080", "Logistics": "080", "Modernization": "080", "Computer": "080", "Wireless": "080",
    "Mysuru City": "0821", "Mysuru Dist": "0821", "Mysuru": "0821", "Chamarajanagar": "08226", "Mandya": "08232", "Hassan": "08172", "Kodagu": "08272",
    "Hubballi Dharwad City": "0836", "Hubballi-Dharwad": "0836", "Dharwad": "0836", "Gadag": "08372", "Haveri": "08375",
    "Mangaluru City": "0824", "Dakshina Kannada": "0824", "Mangaluru": "0824", "Udupi": "0820", "Chikkamagaluru": "08262",
    "Belagavi City": "0831", "Belagavi Dist": "0831", "Belagavi": "0831", "Bagalkote": "08354", "Vijayapura": "08352",
    "Kalaburagi City": "08472", "Kalaburagi": "08472", "Bidar": "08482", "Yadgir": "08473",
    "Ballari": "08392", "Vijayanagara": "08394", "Raichur": "08532", "Koppal": "08539",
    "Davanagere": "08192", "Chitradurga": "08194", "Shivamogga": "08182",
    "Tumakuru": "0816", "Kolar": "08152", "Chikkaballapura": "08156"
};

function cleanPhone(val) {
    if (!val) return "";
    return String(val).replace(/\s/g, '').replace(/—/g, '');
}

function getStdForDist(distKey) {
    const dk = String(distKey || "").trim();
    for (const key of Object.keys(stdMapping)) {
        if (dk.includes(key)) return stdMapping[key];
    }
    return "";
}

async function deepCleanup() {
    const workbook = new ExcelJS.Workbook();
    const headers = ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"];

    let fixCount = 0;

    for (const sheetName of wbSource.SheetNames) {
        const data = xlsx.utils.sheet_to_json(wbSource.Sheets[sheetName]);
        const worksheet = workbook.addWorksheet(sheetName);
        worksheet.columns = headers.map(h => ({ header: h, key: h, width: 20 }));

        data.forEach(row => {
            // 1. Collect all potential numbers
            const rawNumbers = [
                row.office1, row['office 2'], row['mobile 1'], row['mobile 2']
            ].filter(v => v && String(v).trim() !== '').map(v => cleanPhone(v));

            let mobiles = [];
            let landlines = [];

            const std = getStdForDist(row.District || row.UNIT || "");

            rawNumbers.forEach(num => {
                // Remove existing STD to re-classify pure number
                let pure = num;
                if (num.startsWith('0') && num.includes('-')) {
                    pure = num.split('-')[1] || "";
                } else if (num.startsWith('0') && num.length > 5) {
                    // Handle 08022211777 case
                    if (num.startsWith('080')) pure = num.substring(3);
                    else pure = num.substring(4); // default for 4-digit std
                }

                if (pure.match(/^[789]\d{9}$/) || pure.length === 10) {
                    mobiles.push(pure);
                } else if (pure.length >= 6 && pure.length <= 8) {
                    landlines.push(pure);
                }
            });

            // Deduplicate
            mobiles = [...new Set(mobiles)];
            landlines = [...new Set(landlines)];

            // 2. Re-assign and Apply STD
            row.office1 = landlines[0] ? (std ? `${std}-${landlines[0]}` : landlines[0]) : "";
            row['office 2'] = landlines[1] ? (std ? `${std}-${landlines[1]}` : landlines[1]) : "";
            row['mobile 1'] = mobiles[0] || "";
            row['mobile 2'] = mobiles[1] || "";

            // 3. Name and Rank shortening (consistency)
            if (row.Name) {
                for (const [key, val] of Object.entries(rankMapping)) {
                    const regex = new RegExp(`\\b${key.replace('.', '\\.')}\\b`, 'gi');
                    row.Name = row.Name.replace(regex, val);
                }
            }
            if (row.Rank) {
                let r = String(row.Rank).toUpperCase().trim();
                for (const [key, val] of Object.entries(rankMapping)) {
                    if (r.includes(key)) {
                        row.Rank = val;
                        break;
                    }
                }
            }

            worksheet.addRow(row);
            fixCount++;
        });

        worksheet.getRow(1).font = { bold: true };
        worksheet.views = [{ state: 'frozen', ySplit: 1 }];
    }

    await workbook.xlsx.writeFile(masterPath);
    console.log(`Deep cleanup complete. Re-evaluated ${fixCount} rows for landline/mobile consistency.`);

    // Update App CSV
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
    console.log('App CSV synced.');
}

deepCleanup().catch(console.error);
