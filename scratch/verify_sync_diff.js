const Module = require('module');
const originalRequire = Module.prototype.require;
Module.prototype.require = function (id) {
    if (id === 'firebase-admin') {
        return {
            initializeApp: () => {},
            credential: { cert: () => {} },
            firestore: () => ({ collection: () => ({ doc: () => {} }) })
        };
    }
    return originalRequire.apply(this, arguments);
};

const fs = require('fs');
const path = require('path');

// Read sync_directory file contents to eval the functions in this context
// We want to avoid calling startSync() automatically, so let's parse and extract everything except startSync
const syncFile = fs.readFileSync(path.join(__dirname, 'sync_directory.js'), 'utf8');

// We can just define the functions by evaluating the file after removing startSync invocation
// Or simply delete the startSync() call at the end of the file.
const codeToEval = syncFile.replace(/startSync\(\)\.catch[\s\S]*/, '');

eval(codeToEval);

// Now we have getDistrict, parseCSV, parseEmail, shortenRange, extractRank, districts, talukToDistrict, etc.
const csvPath = path.join(__dirname, '..', 'KSP_Contacts_Master_Clean.csv');
const jsonPath = path.join(__dirname, '..', 'officers_real_db.json');

const webContacts = parseCSV(csvPath);
const dbContacts = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));

const dbByEmail = new Map();
const dbByCompound3 = new Map(); // name | unit | subDivision
const dbByCompound2 = new Map(); // name | unit
const dbByName = new Map();      // name

const addToMapArray = (map, key, record) => {
    if (!key) return;
    const k = key.trim().toLowerCase();
    if (!map.has(k)) map.set(k, []);
    map.get(k).push(record);
};

dbContacts.forEach(r => {
    const name = (r.name || '').trim();
    const unit = (r.unit || '').trim();
    const subDivision = (r.subDivision || '').trim();
    if (r.email && r.email.trim()) addToMapArray(dbByEmail, r.email, r);
    if (r.email2 && r.email2.trim()) addToMapArray(dbByEmail, r.email2, r);
    addToMapArray(dbByCompound3, `${name}|${unit}|${subDivision}`, r);
    addToMapArray(dbByCompound2, `${name}|${unit}`, r);
    addToMapArray(dbByName, name, r);
});

const matchedDocs = new Set();
const changes = [];

webContacts.forEach(web => {
    const webEmail = (web.Email || '').split(',')[0].trim().toLowerCase();
    const webDesig = (web.Designation || '').trim().toLowerCase();
    
    let matched = null;
    const findMatch = () => {
        if (webEmail) {
            const candidates = dbByEmail.get(webEmail) || [];
            const found = candidates.find(c => !matchedDocs.has(c.docId));
            if (found) return found;
        }
        const c3Key = `${webDesig}|${shortenRange(web.Unit).toLowerCase()}|${web.Section.toLowerCase()}`;
        const candidates3 = dbByCompound3.get(c3Key) || [];
        const found3 = candidates3.find(c => !matchedDocs.has(c.docId));
        if (found3) return found3;
        
        const c2Key = `${webDesig}|${shortenRange(web.Unit).toLowerCase()}`;
        const candidates2 = dbByCompound2.get(c2Key) || [];
        const found2 = candidates2.find(c => !matchedDocs.has(c.docId));
        if (found2) return found2;
        
        const candidates1 = dbByName.get(webDesig) || [];
        const found1 = candidates1.find(c => !matchedDocs.has(c.docId));
        if (found1) return found1;
        return null;
    };

    matched = findMatch();
    if (matched) {
        matchedDocs.add(matched.docId);
        const emailInfo = parseEmail(web.Email);
        const newDist = getDistrict(web.Designation, web.Unit, web.Section, matched.district, emailInfo.email);
        const newRank = normalizeRank(extractRank(web.Designation));
        
        if (newDist !== matched.district || newRank !== matched.rank) {
            changes.push({
                name: matched.name,
                unit: matched.unit,
                email: emailInfo.email,
                oldDist: matched.district,
                newDist: newDist,
                oldRank: matched.rank,
                newRank: newRank,
                agid: matched.agid
            });
        }
    }
});

console.log(`Total records changing: ${changes.length}`);
console.log('Sample changes:');
changes.slice(0, 50).forEach(c => {
    const parts = [];
    if (c.oldRank !== c.newRank) parts.push(`Rank: ${c.oldRank} -> ${c.newRank}`);
    if (c.oldDist !== c.newDist) parts.push(`District: ${c.oldDist} -> ${c.newDist}`);
    console.log(`[${c.agid}] Name: "${c.name}" | Unit: "${c.unit}" | Email: "${c.email}" => ${parts.join('; ')}`);
});
