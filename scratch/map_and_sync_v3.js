const xlsx = require('xlsx');
const fs = require('fs');
const path = require('path');
const ExcelJS = require('exceljs');

const v3Path = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const currentDataPath = path.join(__dirname, '..', 'officers_v2_current.json');

function normalize(s) {
    if (!s) return "";
    return String(s).toLowerCase()
        .replace(/retd\.?\s*/g, '')
        .replace(/head of police force \(hopf\),?\s*/g, '')
        .replace(/karnataka state/g, '')
        .replace(/[^a-z0-9]/g, '');
}

async function syncV3() {
    console.log("Starting ID mapping and sync for V3 with fuzzy match...");
    
    const currentData = JSON.parse(fs.readFileSync(currentDataPath, 'utf8'));
    const idMap = new Map();
    const mobileMap = new Map();
    let maxId = 0;

    currentData.forEach(r => {
        const nameKey = normalize(r.name);
        const mobileKey = String(r.mobile || "").replace(/\D/g, '').slice(-10);
        
        if (nameKey) idMap.set(nameKey, r.agid);
        if (mobileKey && mobileKey.length === 10) mobileMap.set(mobileKey, r.agid);
        
        const numPart = parseInt(r.agid.replace('KSP', ''));
        if (!isNaN(numPart) && numPart > maxId) maxId = numPart;
    });

    const wb = xlsx.readFile(v3Path);
    const masterSheetName = 'MASTER_MERGED_FINAL';
    const rows = xlsx.utils.sheet_to_json(wb.Sheets[masterSheetName]);
    
    let matched = 0;
    let newRecords = 0;

    rows.forEach(row => {
        const nameKey = normalize(row.Name);
        const mobileKey = String(row['mobile 1'] || "").replace(/\D/g, '').slice(-10);
        
        if (idMap.has(nameKey)) {
            row.agid = idMap.get(nameKey);
            matched++;
        } else if (mobileMap.has(mobileKey)) {
            row.agid = mobileMap.get(mobileKey);
            matched++;
        } else {
            maxId++;
            row.agid = `KSP${maxId.toString().padStart(4, '0')}`;
            newRecords++;
        }
    });

    console.log(`Mapping Result: Matched ${matched}, New ${newRecords}. Total ${rows.length}`);

    const workbook = new ExcelJS.Workbook();
    const headers = ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"];

    const wsMaster = workbook.addWorksheet(masterSheetName);
    wsMaster.columns = headers.map(h => ({ header: h, key: h, width: 20 }));
    rows.forEach(r => wsMaster.addRow(r));
    wsMaster.getRow(1).font = { bold: true };
    wsMaster.views = [{ state: 'frozen', ySplit: 1 }];

    const units = [...new Set(rows.map(r => r.UNIT))];
    units.forEach(u => {
        if (!u) return;
        const sheetName = String(u).substring(0, 31).replace(/[\\\/\?\*\[\]]/g, "");
        if (sheetName === masterSheetName) return;
        const ws = workbook.addWorksheet(sheetName);
        ws.columns = headers.map(h => ({ header: h, key: h, width: 20 }));
        rows.filter(r => r.UNIT === u).forEach(r => ws.addRow(r));
        ws.getRow(1).font = { bold: true };
        ws.views = [{ state: 'frozen', ySplit: 1 }];
    });

    await workbook.xlsx.writeFile(v3Path);
    console.log(`Updated Excel file saved: ${v3Path}`);

    const appData = rows.map(r => {
        const blobParts = [r.Name, r.Rank, r.station, r.UNIT, r.District, r.Section, r.office1, r['office 2'], r['mobile 1'], r['mobile 2'], r.email1, r.email2]
            .filter(v => v && String(v).trim() !== '').map(v => String(v).toLowerCase());
        return {
            agid: r.agid,
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
    const csvOutputPath = path.join(__dirname, '..', 'KSP_Officers_App_FINAL.csv');
    fs.writeFileSync(csvOutputPath, csvContent, 'utf8');
    console.log(`App CSV saved: ${csvOutputPath}`);
}

syncV3().catch(console.error);
