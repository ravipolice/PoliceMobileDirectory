const xlsx = require('xlsx');
const fs = require('fs');
const path = require('path');
const ExcelJS = require('exceljs');

const v3Path = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const realDataPath = path.join(__dirname, '..', 'officers_real_db.json');

function normalize(s) {
    if (!s) return "";
    return String(s).toLowerCase()
        .replace(/retd\.?\s*/g, '')
        .replace(/head of police force \(hopf\),?\s*/g, '')
        .replace(/karnataka state/g, '')
        .replace(/[^a-z0-9]/g, '');
}

async function mergeV3ToReal() {
    console.log("Re-Merging V3 with fallback District logic...");
    
    const realData = JSON.parse(fs.readFileSync(realDataPath, 'utf8'));
    const realIdMap = new Map();
    const realMobileMap = new Map();
    
    let nextAgidNum = 3001; 
    const nums = realData.map(r => r.agid).filter(a => String(a).startsWith('AGID')).map(a => parseInt(a.replace('AGID', ''))).filter(n => !isNaN(n));
    if (nums.length > 0) {
        const maxReal = Math.max(...nums);
        if (maxReal >= nextAgidNum) nextAgidNum = maxReal + 1;
    }

    realData.forEach(r => {
        const nameKey = normalize(r.name);
        const mobileKey = String(r.mobile || "").replace(/\D/g, '').slice(-10);
        const info = { docId: r.docId, agid: r.agid };
        if (nameKey) realIdMap.set(nameKey, info);
        if (mobileKey && mobileKey.length === 10) realMobileMap.set(mobileKey, info);
    });

    const wb = xlsx.readFile(v3Path);
    const rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);
    
    const usedAgids = new Set();
    
    const mergedData = rows.map(row => {
        const nameKey = normalize(row.Name);
        const mobileKey = String(row['mobile 1'] || "").replace(/\D/g, '').slice(-10);
        
        let targetAgid = "";
        let targetDocId = "";

        if (realIdMap.has(nameKey)) {
            const info = realIdMap.get(nameKey);
            targetAgid = info.agid;
            targetDocId = info.docId;
        } else if (mobileKey && mobileKey.length === 10 && realMobileMap.has(mobileKey)) {
            const info = realMobileMap.get(mobileKey);
            targetAgid = info.agid;
            targetDocId = info.docId;
        }
        
        // Ensure inherited AGID is strictly properly formatted AND unique
        if (!targetAgid || !String(targetAgid).match(/^AGID\d+$/) || usedAgids.has(targetAgid)) {
            targetAgid = `AGID${String(nextAgidNum).padStart(4, '0')}`;
            targetDocId = targetAgid;
            nextAgidNum++;
        }

        usedAgids.add(targetAgid);
        row.agid = targetAgid;

        // Fallback for District if empty
        let district = row.District || "";
        if (!district) {
            if (row.UNIT && row.UNIT !== 'Others') {
                district = row.UNIT;
            } else if (row.Range) {
                district = row.Range;
            } else {
                district = "Statelevel";
            }
        }
        row.District = district;

        const blobParts = [row.Name, row.Rank, row.station, row.UNIT, row.District, row.Section, row.office1, row['office 2'], row['mobile 1'], row['mobile 2'], row.email1, row.email2]
            .filter(v => v && String(v).trim() !== '').map(v => String(v).toLowerCase());

        return {
            docId: targetDocId,
            agid: targetAgid,
            name: row.Name || '',
            rank: row.Rank || '',
            office: row.station || '',
            unit: row.UNIT || '',
            district: district,
            subDivision: row.Section || '',
            landline: row.office1 || '',
            landline2: row['office 2'] || '',
            mobile: row['mobile 1'] || '',
            mobile2: row['mobile 2'] || '',
            email: row.email1 || '',
            email2: row.email2 || '',
            searchBlob: [...new Set(blobParts)].join(' '),
            updatedAt: new Date().toISOString()
        };
    });

    console.log(`Merged ${mergedData.length} records. All records now have a District value.`);

    const workbook = new ExcelJS.Workbook();
    const headers = ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"];
    const ws = workbook.addWorksheet('MASTER_MERGED_FINAL');
    ws.columns = headers.map(h => ({ header: h, key: h, width: 20 }));
    rows.forEach(r => ws.addRow(r));
    ws.getRow(1).font = { bold: true };
    ws.views = [{ state: 'frozen', ySplit: 1 }];

    const uniqueUnits = [...new Set(rows.map(r => r.UNIT))];
    uniqueUnits.forEach(u => {
        const sheetName = String(u).substring(0, 31).replace(/[\\\/\?\*\[\]]/g, "");
        const wsUnit = workbook.addWorksheet(sheetName);
        wsUnit.columns = headers.map(h => ({ header: h, key: h, width: 20 }));
        rows.filter(r => r.UNIT === u).forEach(r => wsUnit.addRow(r));
        wsUnit.getRow(1).font = { bold: true };
        wsUnit.views = [{ state: 'frozen', ySplit: 1 }];
    });

    await workbook.xlsx.writeFile(v3Path);

    fs.writeFileSync(path.join(__dirname, '..', 'officers_merge_ready.json'), JSON.stringify(mergedData, null, 2));
    console.log("Updated files saved.");
}

mergeV3ToReal().catch(console.error);
