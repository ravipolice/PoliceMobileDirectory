const xlsx = require('xlsx');
const fs = require('fs');
const path = require('path');
const ExcelJS = require('exceljs');

const v3Path = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');

const uppercaseWords = new Set([
    'DG', 'IGP', 'ADGP', 'DGP', 'DIGP', 'DIG', 'SP', 'DCP', 'DySP', 'DVP', 'IG', 
    'SPL', 'SEC', 'GOVT', 'INDIA', 'CM', 'MD', 'KSPH', 'IDCL', 'BMTC', 'RERA', 
    'KAT', 'SIT', 'CID', 'IPS', 'KLA', 'ANF', 'CCT', 'SDRF', 'HRM', 'L&O'
]);

function formatRetiredName(name) {
    name = name.replace(/\s+/g, ' ');
    const words = name.split(' ');
    const formattedWords = words.map(word => {
        let cleanWord = word.replace(/^[^\w\&\/]+|[^\w\&\/]+$/g, '');
        let cleanUpper = cleanWord.toUpperCase();

        if (uppercaseWords.has(cleanUpper)) {
            return word.toUpperCase();
        }

        let lowered = word.toLowerCase();
        let processed = lowered.replace(/(?:^|[^a-zA-Z0-9])([a-z])/g, function(match, char) {
            return match.toUpperCase();
        });
        
        processed = processed.replace(/\bRetd\b/g, 'Retd');
        processed = processed.replace(/\bDr\b/g, 'Dr');
        
        return processed;
    });

    return formattedWords.join(' ').replace(/\bHon'Ble\b/g, "Hon'ble");
}

async function cleanNames() {
    console.log("Starting final name cleaning pass (Removing Brackets)...");
    const wb = xlsx.readFile(v3Path);
    const rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);

    let fixedCount = 0;

    rows.forEach((r, index) => {
        if (!r.agid || String(r.agid).trim() === '') {
            r.agid = `KSP${String(index + 1).padStart(4, '0')}`;
        }
        let name = String(r.Name || "").trim();
        let rank = String(r.Rank || "").trim();
        let district = String(r.District || "").trim();
        let unit = String(r.UNIT || "").trim();

        // 1. Remove ALL Brackets () and []
        if (name.includes('(') || name.includes(')') || name.includes('[') || name.includes(']')) {
            name = name.replace(/[\[\]\(\)]/g, '').trim();
            fixedCount++;
        }

        // 2. Format AAO and AO designations based on Range, District, and Special Units
        const isAAOorAO = rank.toLowerCase() === 'aao' || rank.toLowerCase() === 'ao';
        if (isAAOorAO) {
            let descriptive = name;
            const unitVal = String(unit || "").trim();
            const distVal = String(district || "").trim();
            const emailVal = String(r.email1 || "").trim();
            const sectionVal = String(r.Section || "").trim();

            if (rank.toUpperCase() === 'AAO') {
                // Formatting rules for AAO
                if (unitVal === 'Training') {
                    if (emailVal.includes('kpa') || descriptive.toUpperCase().includes('KPA') || sectionVal.toUpperCase().includes('KPA')) {
                        descriptive = "AAO KPA";
                    } else {
                        descriptive = "AAO Training";
                    }
                } else if (unitVal === 'Computer') {
                    descriptive = "AAO PCW / SCRB";
                } else if (unitVal === 'Wireless') {
                    descriptive = "AAO CLM";
                } else if (unitVal === 'KSPH') {
                    descriptive = "AAO KSPH & IDCL";
                } else if (unitVal === 'Recruitment') {
                    descriptive = "AAO Recruitment";
                } else if (unitVal === 'Intelligence') {
                    descriptive = "AAO Intelligence";
                } else if (unitVal === 'Railway') {
                    descriptive = "AAO Railways";
                } else if (unitVal === 'KSRP') {
                    if (descriptive.toUpperCase().includes('XII') || sectionVal.toUpperCase().includes('XII')) {
                        descriptive = "AAO XII-BN KSRP";
                    } else {
                        descriptive = "AAO KSRP";
                    }
                } else if (sectionVal === 'GC, BUILDING, ABY SECTION') {
                    descriptive = "AAO GC";
                } else if (unitVal === 'L&O') {
                    if (distVal) {
                        if (distVal.includes("Range")) {
                            const rangePart = distVal.split(/[-–,/]/)[0].trim();
                            descriptive = "AAO " + rangePart;
                        } else {
                            descriptive = "AAO " + distVal;
                        }
                    } else {
                        descriptive = "AAO";
                    }
                } else {
                    if (descriptive.startsWith("AAO")) {
                        descriptive = descriptive.replace(/,\s*/g, ' ').replace(/\s+/g, ' ').trim();
                    } else if (unitVal) {
                        descriptive = "AAO " + unitVal;
                    } else {
                        descriptive = "AAO";
                    }
                }
            } else if (rank.toUpperCase() === 'AO') {
                // Formatting rules for AO
                if (unitVal === 'CID') {
                    descriptive = "AO CID";
                } else if (unitVal === 'DCRE') {
                    descriptive = "AO DCRE";
                } else if (sectionVal === 'CAO') {
                    descriptive = "CAO";
                } else if (distVal === 'Bengaluru City' && (descriptive.toUpperCase().includes('COP') || sectionVal.toUpperCase().includes('COP'))) {
                    descriptive = "AO COP";
                } else {
                    if (distVal) {
                        descriptive = "AO " + distVal;
                    } else if (unitVal && unitVal !== 'L&O') {
                        descriptive = "AO " + unitVal;
                    } else {
                        descriptive = "AO";
                    }
                }
            }

            if (descriptive !== name) {
                name = descriptive;
                fixedCount++;
            }
        } else if (name.toLowerCase() === rank.toLowerCase()) {
            let descriptive = rank;
            if (unit && unit !== 'Others' && unit !== 'L&O') descriptive += " " + unit;
            if (district && district !== unit) descriptive += " " + district;
            name = descriptive.trim();
            fixedCount++;
        }

        // 3. Fix double spaces
        if (name.includes('  ')) {
            name = name.replace(/\s+/g, ' ');
            fixedCount++;
        }

        // 4. Strip redundant rank from name if it's already in the rank field (skip for retired officers to keep full name)
        if (rank && name.endsWith(rank) && !/retd/i.test(rank)) {
            const stripped = name.substring(0, name.length - rank.length).trim();
            if (stripped.length > 2) {
                name = stripped;
                fixedCount++;
            }
        }

        // 5. Convert retired officer names to sentence/title case
        if (/retd|retired/i.test(name) || /retd|retired/i.test(rank) || /retired/i.test(unit)) {
            const formatted = formatRetiredName(name);
            if (formatted !== name) {
                name = formatted;
                fixedCount++;
            }
        }

        r.Name = name;
    });

    console.log(`Cleaned ${fixedCount} records.`);

    // Save back to Excel
    const workbook = new ExcelJS.Workbook();
    const headers = ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"];
    const ws = workbook.addWorksheet('MASTER_MERGED_FINAL');
    ws.columns = headers.map(h => ({ header: h, key: h, width: 20 }));
    rows.forEach(r => ws.addRow(r));
    ws.getRow(1).font = { bold: true };
    await workbook.xlsx.writeFile(v3Path);

    // Prepare for re-upload
    const uploadData = rows.map(r => {
        const blobParts = [r.Name, r.Rank, r.station, r.UNIT, r.District, r.Section, r.office1, r['office 2'], r['mobile 1'], r['mobile 2'], r.email1, r.email2]
            .filter(v => v && String(v).trim() !== '').map(v => String(v).toLowerCase());

        return {
            docId: r.agid,
            agid: r.agid,
            name: r.Name,
            rank: r.Rank,
            office: r.station || '',
            unit: r.UNIT || '',
            district: r.District || '',
            subDivision: r.Section || '',
            landline: r.office1 || '',
            landline2: r['office 2'] || '',
            mobile: r['mobile 1'] || '',
            mobile2: r['mobile 2'] || '',
            email: r.email1 || '',
            email2: r.email2 || '',
            searchBlob: [...new Set(blobParts)].join(' '),
            updatedAt: new Date().toISOString()
        };
    });

    fs.writeFileSync(path.join(__dirname, '..', 'officers_merge_ready.json'), JSON.stringify(uploadData, null, 2));
    console.log("Prepared bracket-free data for re-upload.");
}

cleanNames().catch(console.error);
