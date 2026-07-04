const fs = require('fs');
const path = require('path');
const xlsx = require('xlsx');
const ExcelJS = require('exceljs');

const cleanPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3_CLEAN.xlsx');
const destPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');

console.log('🔄 Copying CLEAN source Excel to V3 target...');
fs.copyFileSync(cleanPath, destPath);
console.log('✅ Excel copied successfully.');

// --- RUN CLEANING LOGIC ---

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

function moveInitialsToEnd(name) {
    const lower = name.toLowerCase();
    let index = lower.indexOf(' retd');
    if (index === -1) {
        index = lower.indexOf(' retired');
    }
    if (index === -1) return name;
    
    let personalPart = name.substring(0, index).trim();
    let retdPart = name.substring(index).trim();
    
    const initialMatch = personalPart.match(/^(Dr\.\s+|Dr\s+)?([A-Z]\.(?:\s*[A-Z]\.?)*)\s*([A-Z][\s\S]+)$/);
    if (initialMatch) {
        const title = initialMatch[1] || "";
        const initials = initialMatch[2].trim();
        const rest = initialMatch[3].trim();
        
        let cleanInitials = initials.split(/\s*[\.\s]\s*/).filter(Boolean).join('.');
        personalPart = `${title}${rest} ${cleanInitials}`.replace(/\s+/g, ' ').trim();
    }
    
    return `${personalPart} ${retdPart}`.replace(/\s+/g, ' ').trim();
}

function extractCorrectRetiredRank(name, originalRank) {
    const nameUpper = name.toUpperCase();
    if (nameUpper.includes('DG & IGP') || nameUpper.includes('DG & IG')) return 'RETD. DG & IGP';
    if (nameUpper.includes('ADGP')) return 'RETD. ADGP';
    if (nameUpper.includes('IGP')) return 'RETD. IGP';
    if (nameUpper.includes('DGP')) return 'RETD. DGP';
    if (nameUpper.includes('DIGP') || nameUpper.includes('DIG')) return 'RETD. DIG';
    if (nameUpper.includes('DCP')) return 'RETD. DCP';
    if (nameUpper.includes('DYSP') || nameUpper.includes('DSP')) return 'RETD. DySP';
    if (nameUpper.includes('SP') || nameUpper.includes('SECRETARY') || nameUpper.includes('CABINET')) return 'RETD. SP';
    return originalRank;
}

async function cleanNames() {
    console.log("Starting final name cleaning pass (Removing Brackets)...");
    const wb = xlsx.readFile(destPath);
    const rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);

    let fixedCount = 0;

    rows.forEach(r => {
        let name = String(r.Name || "").trim();
        let rank = String(r.Rank || "").trim();
        let district = String(r.District || "").trim();
        let unit = String(r.UNIT || "").trim();

        const isRetired = /retd|retired/i.test(name) || /retd|retired/i.test(rank) || /retired/i.test(unit);
        if (isRetired) {
            const correctedRank = extractCorrectRetiredRank(name, rank);
            if (correctedRank !== rank) {
                rank = correctedRank;
                r.Rank = rank;
                fixedCount++;
            }
        }

        // 1. Remove ALL Brackets () and []
        if (name.includes('(') || name.includes(')') || name.includes('[') || name.includes(']')) {
            name = name.replace(/[\[\]\(\)]/g, '').trim();
            fixedCount++;
        }

        // 2. Fix redundant Name/Rank: If Name is just the Rank, make it descriptive
        if (name.toLowerCase() === rank.toLowerCase()) {
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

        // 5. Convert retired officer names to sentence/title case and move initials to end
        if (/retd|retired/i.test(name) || /retd|retired/i.test(rank) || /retired/i.test(unit)) {
            let formatted = formatRetiredName(name);
            formatted = moveInitialsToEnd(formatted);
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
    await workbook.xlsx.writeFile(destPath);

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
    console.log("Prepared bracket-free data with sentence-cased retired officer names for re-upload.");
}

cleanNames().catch(console.error);
