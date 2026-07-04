const fs = require('fs');
const path = require('path');

// Paths to datasets
const csvPath = path.join(__dirname, '..', 'KSP_Contacts_Master_Clean.csv');
const jsonPath = path.join(__dirname, '..', 'officers_real_db.json');

console.log('🔍 Starting cross-check comparison...');

// Helper to parse CSV without external dependencies
function parseCSV(filePath) {
    const content = fs.readFileSync(filePath, 'utf8');
    const lines = content.split(/\r?\n/);
    const records = [];

    if (lines.length === 0) return [];

    // Strip BOM and split
    const headerLine = lines[0].replace(/^\uFEFF/, '');
    const cleanHeaders = headerLine.split(',').map(h => h.replace(/^"|"$/g, '').trim());

    for (let i = 1; i < lines.length; i++) {
        const line = lines[i].trim();
        if (!line) continue;

        const matches = [];
        let insideQuote = false;
        let entry = '';
        for (let j = 0; j < line.length; j++) {
            const char = line[j];
            if (char === '"') {
                insideQuote = !insideQuote;
            } else if (char === ',' && !insideQuote) {
                matches.push(entry.trim());
                entry = '';
            } else {
                entry += char;
            }
        }
        matches.push(entry.trim());

        const record = {};
        cleanHeaders.forEach((header, index) => {
            let val = matches[index] ? matches[index].replace(/^"|"$/g, '').trim() : '';
            record[header] = val;
        });
        records.push(record);
    }
    return records;
}

// Helper to shorten range names (e.g. Northern Range -> NR)
function shortenRange(unitName) {
    if (!unitName) return '';
    return unitName
        .replace(/\bNorth-Eastern Range\b/gi, 'NER')
        .replace(/\bNorth Eastern Range\b/gi, 'NER')
        .replace(/\bNortheastern Range\b/gi, 'NER')
        .replace(/\bCentral Range\b/gi, 'CR')
        .replace(/\bWestern Range\b/gi, 'WR')
        .replace(/\bNorthern Range\b/gi, 'NR')
        .replace(/\bSouthern Range\b/gi, 'SR')
        .replace(/\bEastern Range\b/gi, 'ER')
        .replace(/\bBallari Range\b/gi, 'BR')
        .replace(/\s*[–-]\s*/g, ', ')
        .trim();
}

try {
    if (!fs.existsSync(csvPath)) {
        console.error(`❌ Website CSV data file not found at: ${csvPath}`);
        process.exit(1);
    }
    if (!fs.existsSync(jsonPath)) {
        console.error(`❌ Firestore JSON data file not found at: ${jsonPath}`);
        process.exit(1);
    }

    // 1. Load Datasets
    const websiteContacts = parseCSV(csvPath);
    const firestoreContacts = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));

    console.log(`📊 Loaded ${websiteContacts.length} contacts from KSP Website CSV.`);
    console.log(`📊 Loaded ${firestoreContacts.length} contacts from Firestore JSON.`);

    // 2. Index Firestore contacts into arrays
    const dbByEmail = new Map();
    const dbByCompound3 = new Map(); // designation | unit | section
    const dbByCompound2 = new Map(); // designation | unit
    const dbByName = new Map();      // designation

    const addToMapArray = (map, key, record) => {
        if (!key) return;
        const k = key.trim().toLowerCase();
        if (!map.has(k)) map.set(k, []);
        map.get(k).push(record);
    };

    firestoreContacts.forEach(record => {
        if (record.email && record.email.trim()) {
            addToMapArray(dbByEmail, record.email, record);
        }
        if (record.email2 && record.email2.trim()) {
            addToMapArray(dbByEmail, record.email2, record);
        }
        const name = (record.name || '').trim().toLowerCase();
        const unit = (record.unit || '').trim().toLowerCase();
        const sub = (record.subDivision || '').trim().toLowerCase();

        addToMapArray(dbByCompound3, name + ' | ' + unit + ' | ' + sub, record);
        addToMapArray(dbByCompound2, name + ' | ' + unit, record);
        addToMapArray(dbByName, name, record);
    });

    const matchedDocIds = new Set();

    const findAndMatch = (map, key, webPhoneStr) => {
        if (!key) return null;
        const k = key.trim().toLowerCase();
        if (!map.has(k)) return null;
        const arr = map.get(k);

        const webPhones = (webPhoneStr || '').replace(/[\s-]/g, '').split(/[\/,]/).filter(Boolean);

        // 1. Try to find a record with a matching phone number
        for (const record of arr) {
            if (!matchedDocIds.has(record.docId)) {
                const dbPhones = [record.mobile, record.mobile2, record.landline, record.landline2]
                    .filter(Boolean)
                    .map(p => p.replace(/[\s-]/g, ''));
                
                let phoneMatch = false;
                for (const wp of webPhones) {
                    if (wp === '—') continue;
                    for (const dbp of dbPhones) {
                        if (dbp && (dbp.includes(wp) || wp.includes(dbp))) {
                            phoneMatch = true;
                            break;
                        }
                    }
                    if (phoneMatch) break;
                }

                if (phoneMatch) {
                    matchedDocIds.add(record.docId);
                    return record;
                }
            }
        }

        // 2. Fallback to first unmatched record
        for (const record of arr) {
            if (!matchedDocIds.has(record.docId)) {
                matchedDocIds.add(record.docId);
                return record;
            }
        }
        return null;
    };

    // 3. Perform Cross-Check
    const missingInFirestore = [];
    const mismatchedContacts = [];
    let matchingEmailsCount = 0;
    let matchingCompound3Count = 0;
    let matchingCompound2Count = 0;
    let matchingNamesCount = 0;

    websiteContacts.forEach(web => {
        const webEmail = (web.Email || '').split(',')[0].trim().toLowerCase();
        const webDesig = (web.Designation || '').trim().toLowerCase();
        const webUnit = shortenRange(web.Unit).toLowerCase();
        const webSec = (web.Section || '').trim().toLowerCase();

        let matchedRecord = null;

        if ((matchedRecord = findAndMatch(dbByCompound3, webDesig + ' | ' + webUnit + ' | ' + webSec, web.Phone))) {
            matchingCompound3Count++;
        } else if ((matchedRecord = findAndMatch(dbByCompound2, webDesig + ' | ' + webUnit, web.Phone))) {
            matchingCompound2Count++;
        } else if (webEmail && (matchedRecord = findAndMatch(dbByEmail, webEmail, web.Phone))) {
            matchingEmailsCount++;
        } else if ((matchedRecord = findAndMatch(dbByName, webDesig, web.Phone))) {
            matchingNamesCount++;
        }

        if (!matchedRecord) {
            missingInFirestore.push(web);
        } else {
            // Check for phone mismatches
            const webPhones = (web.Phone || '').replace(/[\s-]/g, '').split(/[\/,]/);
            const dbPhones = [matchedRecord.mobile, matchedRecord.mobile2, matchedRecord.landline, matchedRecord.landline2]
                .filter(Boolean)
                .map(p => p.replace(/[\s-]/g, ''));

            let phoneMatch = false;
            for (const wp of webPhones) {
                if (!wp || wp === '—') continue;
                for (const dbp of dbPhones) {
                    if (dbp && (dbp.includes(wp) || wp.includes(dbp))) {
                        phoneMatch = true;
                        break;
                    }
                }
            }

            if (!phoneMatch && web.Phone && web.Phone !== '—' && dbPhones.length > 0) {
                mismatchedContacts.push({
                    designation: web.Designation,
                    webPhone: web.Phone,
                    dbPhones: dbPhones.join(' / '),
                    email: web.Email
                });
            }
        }
    });

    // 4. Print Summary Report
    console.log('\n════════════════════════════════════════════════');
    console.log('🏁 CROSS-CHECK COMPLETE SUMMARY');
    console.log('════════════════════════════════════════════════');
    console.log(`✅ Matches found by Email:            ${matchingEmailsCount}`);
    console.log(`✅ Matches found by Name+Unit+Section: ${matchingCompound3Count}`);
    console.log(`✅ Matches found by Name+Unit:        ${matchingCompound2Count}`);
    console.log(`✅ Matches found by Designation:      ${matchingNamesCount}`);
    console.log(`❌ Website contacts missing in DB:     ${missingInFirestore.length}`);
    console.log(`⚠️ Contacts with phone mismatch:      ${mismatchedContacts.length}`);
    console.log('════════════════════════════════════════════════\n');

    if (missingInFirestore.length > 0) {
        console.log('📋 SAMPLE MISSING IN FIRESTORE (First 5):');
        missingInFirestore.slice(0, 5).forEach((item, index) => {
            console.log(`  ${index + 1}. [${item.Unit}] ${item.Designation} - Phone: ${item.Phone || 'N/A'}, Email: ${item.Email || 'N/A'}`);
        });
        console.log('');
    }

    if (mismatchedContacts.length > 0) {
        console.log('📋 SAMPLE PHONE NUMBER MISMATCHES (First 5):');
        mismatchedContacts.slice(0, 5).forEach((item, index) => {
            console.log(`  ${index + 1}. ${item.designation} \n     - Website: ${item.webPhone} \n     - Firestore: ${item.dbPhones}`);
        });
        console.log('');
    }

} catch (err) {
    console.error('❌ Error during cross-check execution:', err);
}
