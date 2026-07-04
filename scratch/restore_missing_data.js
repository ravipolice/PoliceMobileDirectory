const xlsx = require('xlsx');
const ExcelJS = require('exceljs');
const path = require('path');
const fs = require('fs');

const v3Path = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const splitsDir = path.join(__dirname, 'temp_splits');

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

function normalizeName(name) {
    if (!name) return "";
    let n = String(name).trim();
    for (const [key, val] of Object.entries(rankMapping)) {
        const regex = new RegExp(`\\b${key.replace('.', '\\.')}\\b`, 'gi');
        n = n.replace(regex, val);
    }
    return n.toLowerCase();
}

const dataMap = new Map();

// Global regex to extract ALL valid emails from a string
const emailRegex = /[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/g;

function cleanPhone(val) {
    if (!val) return "";
    return String(val).replace(/\s/g, '').replace(/—/g, '');
}

console.log("Reading CSV splits to recover data...");
const files = fs.readdirSync(splitsDir).filter(f => f.endsWith('.csv'));

files.forEach(file => {
    const content = fs.readFileSync(path.join(splitsDir, file), 'utf8');
    const rows = xlsx.utils.sheet_to_json(xlsx.read(content, { type: 'string' }).Sheets.Sheet1);
    
    rows.forEach(r => {
        // Normalize the CSV Name using the same logic as V3
        const name = normalizeName(r.Name);
        if (name) {
            // Extract all emails
            let emails = [];
            if (r.Email) {
                let matches = String(r.Email).match(emailRegex);
                if (matches) emails = matches.map(e => e.toLowerCase());
            }

            dataMap.set(name, {
                office1: cleanPhone(r['Office 1']),
                office2: cleanPhone(r['Office 2']),
                mobile1: cleanPhone(r['Mobile 1']),
                mobile2: cleanPhone(r['Mobile 2']),
                email1: emails[0] || "",
                email2: emails[1] || ""
            });
        }
    });
});

console.log(`Recovered data for ${dataMap.size} unique Names from CSV splits.`);

// Debug: Print first 5 entries of dataMap
let count = 0;
for (const [k, v] of dataMap) {
    if (v.mobile2) {
        console.log(`Sample Recovered: [${k}] -> m2: ${v.mobile2}`);
        if (++count > 5) break;
    }
}

async function restore() {
    const wbSource = xlsx.readFile(v3Path);
    const workbook = new ExcelJS.Workbook();
    const headers = ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"];

    let totalUpdated = 0;

    for (const sheetName of wbSource.SheetNames) {
        const data = xlsx.utils.sheet_to_json(wbSource.Sheets[sheetName]);
        const worksheet = workbook.addWorksheet(sheetName);
        worksheet.columns = headers.map(h => ({ header: h, key: h, width: 20 }));

        data.forEach(row => {
            const name = normalizeName(row.Name);
            const recovered = dataMap.get(name);
            
            if (recovered) {
                // Debug: Check a known entry
                if (name.includes('hopp') || name.includes('hopf')) {
                     console.log(`Matching HoPF: ${name} | recovered m2: ${recovered.mobile2} | current m2: ${row['mobile 2']}`);
                }
                // Only update if current is empty or we have better data
                // For mobiles and emails, we prioritize recovering the second entry
                if (recovered.mobile2 && (!row['mobile 2'] || row['mobile 2'] === "")) {
                    row['mobile 2'] = recovered.mobile2;
                    totalUpdated++;
                }
                if (recovered.email2 && (!row.email2 || row.email2 === "")) {
                    row.email2 = recovered.email2;
                }
                
                // Also restore office 2 if missing
                if (recovered.office2 && (!row['office 2'] || row['office 2'] === "")) {
                    row['office 2'] = recovered.office2;
                }

                // If mobile 1 or office 1 were missing in V3 but exist in splits, restore them
                if (recovered.mobile1 && (!row['mobile 1'] || row['mobile 1'] === "")) {
                    row['mobile 1'] = recovered.mobile1;
                }
                if (recovered.office1 && (!row.office1 || row.office1 === "")) {
                    row.office1 = recovered.office1;
                }
                if (recovered.email1 && (!row.email1 || row.email1 === "")) {
                    row.email1 = recovered.email1;
                }
            }
            
            worksheet.addRow(row);
        });

        worksheet.getRow(1).font = { bold: true };
        worksheet.views = [{ state: 'frozen', ySplit: 1 }];
    }

    await workbook.xlsx.writeFile(v3Path);
    console.log(`Restored missing data into ${v3Path}. Updated ${totalUpdated} records with second mobile numbers.`);
}

restore().catch(console.error);
