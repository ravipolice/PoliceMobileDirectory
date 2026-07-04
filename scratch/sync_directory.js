const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// 1. Initialize Firebase Admin
const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const collectionRef = db.collection('officers');

// Paths
const csvPath = path.join(__dirname, '..', 'KSP_Contacts_Master_Clean.csv');
const jsonPath = path.join(__dirname, '..', 'officers_real_db.json');

// Command line flag
const commitChanges = process.argv.includes('--commit');

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

// Helper to parse phone numbers (split landlines and mobiles)
function parsePhone(rawPhone) {
    let landline = '';
    let landline2 = '';
    let mobile = '';
    let mobile2 = '';

    if (!rawPhone || rawPhone === '—') {
        return { landline, landline2, mobile, mobile2 };
    }

    const parts = rawPhone.split('/');
    let rawLandlines = [];
    let rawMobiles = [];

    if (parts.length > 1) {
        // First part is landlines, second is mobiles
        rawLandlines = parts[0].split(',').map(s => s.trim()).filter(Boolean);
        rawMobiles = parts[1].split(',').map(s => s.trim()).filter(Boolean);
    } else {
        // No slash, try to classify each number
        const numbers = rawPhone.split(',').map(s => s.trim()).filter(Boolean);
        numbers.forEach(num => {
            const cleanNum = num.replace(/[\s-]/g, '');
            if (/^[6-9]\d{9}$/.test(cleanNum) || (cleanNum.length === 10 && ['6','7','8','9'].includes(cleanNum[0]))) {
                rawMobiles.push(num);
            } else {
                rawLandlines.push(num);
            }
        });
    }

    const cleanNumStr = (num) => {
        return num.trim().replace(/^RES-\s*/i, '').replace(/[–-]F$/i, '');
    };

    if (rawLandlines.length > 0) landline = cleanNumStr(rawLandlines[0]);
    if (rawLandlines.length > 1) landline2 = cleanNumStr(rawLandlines[1]);
    if (rawMobiles.length > 0) mobile = cleanNumStr(rawMobiles[0]);
    if (rawMobiles.length > 1) mobile2 = cleanNumStr(rawMobiles[1]);

    return { landline, landline2, mobile, mobile2 };
}

// Helper to parse email addresses
function parseEmail(rawEmail) {
    let email = '';
    let email2 = '';

    if (!rawEmail) {
        return { email, email2 };
    }

    const emails = rawEmail.split(/[,\s]+/).map(s => s.trim()).filter(s => s.includes('@'));
    if (emails.length > 0) email = emails[0];
    if (emails.length > 1) email2 = emails[1];

    return { email, email2 };
}

// Helper to shorten range names (e.g. Northern Range -> NR) and functional units
function shortenRange(unitName) {
    if (!unitName) return '';
    let u = unitName
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

    // Short codes for specific units
    if (u === 'Communication, Logistics & Modernisation') return 'CLM';
    if (u === 'Karnataka Police Academy (KPA)') return 'KPA';
    if (u === 'Police Training Wing') return 'Training';
    if (u === 'Police Recruitment Wing') return 'Recruitment';
    if (u === 'Karnataka Railways Police') return 'Railways';
    if (u === 'Police Computer Wing (PCW) / SCRB') return 'PCW / SCRB';
    if (u === 'State Intelligence') return 'Intelligence';
    if (u === 'Directorate of Civil Rights Enforcement (DCRE)') return 'DCRE';
    if (u === 'Criminal Investigation Department (CID)') return 'CID';
    if (u === 'Internal Security Division (ISD)') return 'ISD';
    if (u === 'Karnataka State Reserve Police (KSRP)') return 'KSRP';

    return u;
}

// Helper to normalize rank names inside designation string (e.g. DSP -> DySP)
function normalizeDesignationName(nameStr) {
    if (!nameStr) return '';
    return nameStr
        .replace(/\b(dysp|dsp|deputy superintendent of police|deputy superintendent)\b/gi, 'DySP')
        .replace(/\b(assistant administrative officer|assistant administrative offr)\b/gi, 'AAO')
        .replace(/\b(chief administrative officer)\b/gi, 'CAO')
        .replace(/\b(administrative officer)\b/gi, 'AO')
        .replace(/\b(police inspector|circle inspector)\b/gi, 'PI')
        .replace(/\b(police sub-inspector|police sub inspector|sub-inspector)\b/gi, 'PSI')
        .replace(/\b(assistant sub-inspector|assistant sub inspector)\b/gi, 'ASI')
        .replace(/\b(head constable)\b/gi, 'HC')
        .replace(/\b(police constable)\b/gi, 'PC')
        .replace(/\b(assistant commissioner of police|assistant commissioner)\b/gi, 'ACP')
        .replace(/\b(deputy commissioner of police|deputy commissioner)\b/gi, 'DCP')
        .replace(/\b(superintendent of police)\b/gi, 'SP')
        .replace(/\b(additional superintendent of police|additional superintendent|addl\.\s*sp)\b/gi, 'Addl.SP')
        .replace(/\b(assistant superintendent of police|assistant superintendent)\b/gi, 'ASP')
        .replace(/\b(deputy commandant)\b/gi, 'DEPT.CMDT')
        .replace(/\b(assistant commandant)\b/gi, 'ASST.CMDT')
        .replace(/\b(commandant)\b/gi, 'CMDT')
        .replace(/\b(director general & inspector general of police|director general and inspector general of police|dg & igp|dg and igp)\b/gi, 'DG & IGP')
        .replace(/\b(additional director general of police|additional director general|adgp)\b/gi, 'ADGP')
        .replace(/\b(deputy inspector general of police|deputy inspector general|digp)\b/gi, 'DIG')
        .replace(/\b(inspector general of police|inspector general|igp)\b/gi, 'IGP')
        .replace(/\b(director general of police)\b/gi, 'DGP')
        .trim();
}

// Rank Normalization Rules according to DIRECTORY_ORGANIZATION_RULES.md
function normalizeRank(rankStr) {
    if (!rankStr) return '';
    let r = rankStr.trim();
    
    const isRetd = r.toUpperCase().startsWith('RETD.');
    let cleanRank = r;
    if (isRetd) {
        cleanRank = r.substring(5).trim();
    }
    
    const d = cleanRank.toLowerCase();
    let normalized = cleanRank;
    
    if (d === 'digp' || d === 'deputy inspector general' || d === 'deputy inspector general of police') normalized = 'DIG';
    else if (d === 'commandant') normalized = 'CMDT';
    else if (d === 'deputy commandant') normalized = 'DEPT.CMDT';
    else if (d === 'assistant commandant') normalized = 'ASST.CMDT';
    else if (d === 'addl. sp' || d === 'addl.sp' || d === 'additional superintendent' || d === 'additional superintendent of police') normalized = 'Addl.SP';
    else if (d === 'dysp' || d === 'deputy superintendent' || d === 'deputy superintendent of police' || d === 'dsp') normalized = 'DySP';
    else if (d === 'asp' || d === 'assistant superintendent' || d === 'assistant superintendent of police') normalized = 'ASP';
    else if (d === 'acp' || d === 'assistant commissioner' || d === 'assistant commissioner of police') normalized = 'ACP';
    else if (d === 'dcp' || d === 'deputy commissioner' || d === 'deputy commissioner of police') normalized = 'DCP';
    else if (d === 'dg & igp' || d === 'director general & inspector general of police') normalized = 'DG & IGP';
    else if (d === 'adgp' || d === 'additional director general' || d === 'additional director general of police') normalized = 'ADGP';
    else if (d === 'igp' || d === 'inspector general' || d === 'inspector general of police') normalized = 'IGP';
    else if (d === 'dgp' || d === 'director general of police') normalized = 'DGP';
    else if (d === 'pi' || d === 'police inspector' || d === 'inspector') normalized = 'PI';
    else if (d === 'psi' || d === 'police sub-inspector' || d === 'sub-inspector') normalized = 'PSI';
    else if (d === 'asi' || d === 'assistant sub-inspector') normalized = 'ASI';
    else if (d === 'hc' || d === 'head constable') normalized = 'HC';
    else if (d === 'pc' || d === 'police constable') normalized = 'PC';
    
    return isRetd ? `RETD. ${normalized}` : normalized;
}

// Rank Extraction Rules
function extractRank(designation) {
    const d = designation.toLowerCase();
    
    // Check administrative/ministerial ranks first to avoid false positives (e.g. AAO IGP Office -> IGP)
    if (d.includes('assistant administrative officer') || /\baao\b/.test(d)) return 'AAO';
    if (d.includes('chief administrative officer') || /\bcao\b/.test(d)) return 'AO';
    if (d.includes('administrative officer') || /\bao\b/.test(d)) return 'AO';
    if (d.includes('first division assistant') || /\bfda\b/.test(d)) return 'FDA';
    if (d.includes('second division assistant') || /\bsda\b/.test(d)) return 'SDA';
    if (d.includes('stenographer') || /\bsteno\b/.test(d)) return 'STENO';
    if (d.includes('typist') || /\btypist\b/.test(d)) return 'TYPIST';
    if (d.includes('personal assistant') || /\bpa\b/.test(d)) return 'PA';
    
    if (d.includes('director general') && d.includes('inspector general')) return 'DG & IGP';
    if (d.includes('additional director general') || /\badgp\b/.test(d)) return 'ADGP';
    if (d.includes('deputy inspector general') || /\bdigp\b/.test(d)) return 'DIG';
    if (d.includes('inspector general') || /\bigp\b/.test(d)) return 'IGP';
    if (d.includes('assistant superintendent') || /\basp\b/.test(d)) return 'ASP';
    if (d.includes('additional superintendent') || /\baddl\.\s*sp\b/.test(d)) return 'Addl.SP';
    if (d.includes('superintendent of police') || /\bsp\b/.test(d)) return 'SP';
    if (d.includes('deputy superintendent') || /\b(dysp|dsp)\b/.test(d)) return 'DySP';
    if (d.includes('assistant commissioner') || /\bacp\b/.test(d)) return 'ACP';
    if (d.includes('deputy commissioner') || /\bdcp\b/.test(d)) return 'DCP';
    if (d.includes('commissioner of police') || /\bcp\b/.test(d)) return 'CP';
    if (d.includes('police inspector') || d.includes('inspector') || /\bpi\b/.test(d)) return 'PI';
    if (d.includes('police sub-inspector') || d.includes('sub-inspector') || /\bpsi\b/.test(d)) return 'PSI';
    if (d.includes('assistant sub-inspector') || /\basi\b/.test(d)) return 'ASI';
    if (d.includes('head constable') || /\bhc\b/.test(d)) return 'HC';
    if (d.includes('police constable') || /\bpc\b/.test(d)) return 'PC';
    if (d.includes('deputy commandant')) return 'DEPT.CMDT';
    if (d.includes('assistant commandant')) return 'ASST.CMDT';
    if (d.includes('commandant')) return 'CMDT';
    
    return 'PI';
}

// District Mapping Data
const talukToDistrict = {
    'Koratagere': 'Tumakuru', 'Madhugiri': 'Tumakuru', 'Pavagada': 'Tumakuru', 'Sira': 'Tumakuru', 
    'Tiptur': 'Tumakuru', 'Gubbi': 'Tumakuru', 'Kunigal': 'Tumakuru', 'Turuvekere': 'Tumakuru', 
    'Chikkanayakanahalli': 'Tumakuru', 'Tumkur': 'Tumakuru', 'Tumakuru': 'Tumakuru',
    'Channapatna': 'Ramanagara', 'Kanakapura': 'Ramanagara', 'Magadi': 'Ramanagara', 'Ramanagar': 'Ramanagara', 'Ramanagara': 'Ramanagara',
    'Bangarapet': 'Kolar', 'Malur': 'Kolar', 'Mulbagal': 'Kolar', 'Srinivaspur': 'Kolar', 'Kolara': 'Kolar', 'Kolar': 'Kolar',
    'Bagepalli': 'Chikkaballapura', 'Chintamani': 'Chikkaballapura', 'Gauribidanur': 'Chikkaballapura', 
    'Gudibanda': 'Chikkaballapura', 'Sidlaghatta': 'Chikkaballapura', 'Chikkaballapur': 'Chikkaballapura', 'Chikkaballapura': 'Chikkaballapura', 'Chikballapur': 'Chikkaballapura',
    'Athani': 'Belagavi', 'Bailhongal': 'Belagavi', 'Chikkodi': 'Belagavi', 'Gokak': 'Belagavi', 
    'Hukkeri': 'Belagavi', 'Khanapur': 'Belagavi', 'Raybag': 'Belagavi', 'Ramdurg': 'Belagavi', 
    'Saundatti': 'Belagavi', 'Belgaum': 'Belagavi', 'Belagavi': 'Belagavi',
    'Maddur': 'Mandya', 'Malavalli': 'Mandya', 'Pandavapura': 'Mandya', 'Srirangapatna': 'Mandya', 'Nagamangala': 'Mandya', 'Krishnarajapet': 'Mandya',
    'Arsikere': 'Hassan', 'Channarayapatna': 'Hassan', 'Holenarasipura': 'Hassan', 'Sakleshpur': 'Hassan', 'Belur': 'Hassan', 'Arkalgud': 'Hassan',
    'Nanjangud': 'Mysuru', 'Hunsur': 'Mysuru', 'T.Narasipura': 'Mysuru', 'Periyapatna': 'Mysuru', 'Mysore': 'Mysuru', 'Mysuru': 'Mysuru',
    'Kadur': 'Chikkamagaluru', 'Koppa': 'Chikkamagaluru', 'Tarikere': 'Chikkamagaluru', 'Mudigere': 'Chikkamagaluru', 'Chikmagalur': 'Chikkamagaluru', 'Chikkamagaluru': 'Chikkamagaluru',
    'Harihara': 'Davanagere', 'Honnali': 'Davanagere', 'Jagalur': 'Davanagere', 'Channagiri': 'Davanagere',
    'Bhadravathi': 'Shivamogga', 'Sagar': 'Shivamogga', 'Shikaripura': 'Shivamogga', 'Soraba': 'Shivamogga', 'Thirthahalli': 'Shivamogga', 'Shimoga': 'Shivamogga', 'Shivamogga': 'Shivamogga',
    'Hospet': 'Vijayanagara', 'Sandur': 'Ballari', 'Siruguppa': 'Ballari', 'Bellary': 'Ballari', 'Ballari': 'Ballari',
    'Indi': 'Vijayapura', 'Sindgi': 'Vijayapura', 'Muddebihal': 'Vijayapura', 'Basavana Bagewadi': 'Vijayapura', 'Bijapur': 'Vijayapura', 'Vijayapura': 'Vijayapura',
    'Jamkhandi': 'Bagalkote', 'Mudhol': 'Bagalkote', 'Badami': 'Bagalkote', 'Hungund': 'Bagalkote', 'Bagalkot': 'Bagalkote', 'Bagalkote': 'Bagalkote',
    'Afzalpur': 'Kalaburagi', 'Aland': 'Kalaburagi', 'Chincholi': 'Kalaburagi', 'Chitapur': 'Kalaburagi', 'Sedam': 'Kalaburagi', 'Shahabad': 'Kalaburagi', 'Gulbarga': 'Kalaburagi', 'Kalaburagi': 'Kalaburagi',
    'Shorapur': 'Yadgiri', 'Shahapur': 'Yadgiri', 'Yadgir': 'Yadgiri', 'Yadgiri': 'Yadgiri',
    'Basavakalyan': 'Bidar', 'Bhalki': 'Bidar', 'Homnabad': 'Bidar', 'Aurad': 'Bidar',
    'Bengaluru': 'Bengaluru City', 'Bangalore': 'Bengaluru City', 'Bengaluru Urban': 'Bengaluru City'
};

const districts = [
    'Bagalkote', 'Ballari', 'Belagavi', 'Bengaluru City', 'Bengaluru Rural', 'Bidar', 
    'Chamarajanagara', 'Chikkaballapura', 'Chikkamagaluru', 'Chitradurga', 'Dakshina Kannada', 
    'Davanagere', 'Dharwad', 'Gadag', 'Hassan', 'Haveri', 'Kalaburagi', 'Kodagu', 'Kolar', 
    'Koppal', 'Mandya', 'Mysuru', 'Mysuru City', 'Raichur', 'Ramanagara', 'Shivamogga', 'Tumakuru', 
    'Udupi', 'Uttara Kannada', 'Vijayapura', 'Yadgiri', 'Koppala', 'Hubballi-Dharwad City', 'Mangaluru City', 'Belagavi City', 'Kalaburagi City'
];

districts.forEach(d => {
    if (!talukToDistrict[d]) talukToDistrict[d] = d;
});

const talukKeys = Object.keys(talukToDistrict).sort((a, b) => b.length - a.length);

const rangeDistricts = {
    'cr': ['bengaluru city', 'bengaluru rural', 'kolar', 'chikkaballapura', 'ramanagara', 'tumakuru'],
    'er': ['davanagere', 'shivamogga', 'haveri', 'chitradurga'],
    'nr': ['belagavi', 'vijayapura', 'bagalkote', 'dharwad', 'gadag', 'hubballi-dharwad city', 'belagavi city'],
    'sr': ['mysuru', 'hassan', 'mandya', 'chamarajanagara', 'kodagu', 'mysuru city'],
    'wr': ['dakshina kannada', 'uttara kannada', 'udupi', 'chikkamagaluru', 'mangaluru city'],
    'ner': ['kalaburagi', 'yadgiri', 'bidar', 'kalaburagi city'],
    'br': ['ballari', 'vijayanagara', 'koppal', 'raichur']
};

function getDistrict(designation, unit, section, fallbackDistrict, email) {
    const des = (designation || '').toLowerCase();
    const un = (unit || '').toLowerCase();
    const sec = (section || '').toLowerCase();
    const em = (email || '').toLowerCase();

    // Determine if unit is a Range Unit
    let unitRangeKey = null;
    if (un.includes('central range') || un.includes('cr,')) unitRangeKey = 'cr';
    else if (un.includes('eastern range') || un.includes('er,')) unitRangeKey = 'er';
    else if (un.includes('northern range') || un.includes('nr,')) unitRangeKey = 'nr';
    else if (un.includes('southern range') || un.includes('sr,')) unitRangeKey = 'sr';
    else if (un.includes('western range') || un.includes('wr,')) unitRangeKey = 'wr';
    else if (un.includes('north-eastern range') || un.includes('north eastern range') || un.includes('northeastern range') || un.includes('ner,')) unitRangeKey = 'ner';
    else if (un.includes('ballari range') || un.includes('br,')) unitRangeKey = 'br';

    // 1. Guess from specific designation keywords first
    for (const key of talukKeys) {
        const regex = new RegExp('\\b' + key.toLowerCase() + '\\b', 'i');
        if (regex.test(des)) return talukToDistrict[key];
    }

    // 2. Guess from email subdomain/pattern (very specific and accurate for range constituent districts)
    if (em) {
        if (em.includes('cbpura')) return 'Chikkaballapura';
        if (em.includes('kgf')) return 'Kolar';
        if (em.includes('rmn')) return 'Ramanagara';
        if (em.includes('tmk') || em.includes('tum')) return 'Tumakuru';
        if (em.includes('klr')) return 'Kolar';
        if (em.includes('dvg')) return 'Davanagere';
        if (em.includes('smg')) return 'Shivamogga';
        if (em.includes('hvr')) return 'Haveri';
        if (em.includes('cta')) return 'Chitradurga';
        if (em.includes('bgm')) return 'Belagavi';
        if (em.includes('bjp')) return 'Vijayapura';
        if (em.includes('bgk')) return 'Bagalkote';
        if (em.includes('dwr') || em.includes('dhw')) return 'Dharwad';
        if (em.includes('gdg')) return 'Gadag';
        if (em.includes('mys')) return 'Mysuru';
        if (em.includes('mdy')) return 'Mandya';
        if (em.includes('chn')) return 'Chamarajanagara';
        if (em.includes('mcr')) return 'Kodagu';
        if (em.includes('hsn')) return 'Hassan';
        if (em.includes('ckm')) return 'Chikkamagaluru';
        if (em.includes('kwr')) return 'Uttara Kannada';
        if (em.includes('udp')) return 'Udupi';
        if (em.includes('maq') || em.includes('mng')) return 'Dakshina Kannada';
        if (em.includes('klb') || em.includes('glb')) return 'Kalaburagi';
        if (em.includes('ydr') || em.includes('ydg')) return 'Yadgiri';
        if (em.includes('bdr')) return 'Bidar';
        if (em.includes('blr') || em.includes('bellary')) return 'Ballari';
        if (em.includes('kpl')) return 'Koppal';
        if (em.includes('rcr')) return 'Raichur';
        if (em.includes('vjn')) return 'Vijayanagara';
        if (em.includes('bng') && unitRangeKey === 'cr') return 'Bengaluru Rural';
    }

    // 3. Check if fallback district is valid.
    // If unit is a range unit, fallback district is only valid if it belongs to that range.
    if (fallbackDistrict) {
        const cleaned = fallbackDistrict.trim();
        const cleanedLower = cleaned.toLowerCase();
        const isWhitelisted = districts.some(d => d.toLowerCase() === cleanedLower) ||
                              cleanedLower === 'statelevel' ||
                              cleanedLower === 'state level';
        if (isWhitelisted) {
            if (!unitRangeKey || rangeDistricts[unitRangeKey].includes(cleanedLower) || cleanedLower === 'statelevel' || cleanedLower === 'state level') {
                return cleaned;
            }
        }
    }

    // 4. Guess from unit/section text
    for (const key of talukKeys) {
        const regex = new RegExp('\\b' + key.toLowerCase() + '\\b', 'i');
        if (regex.test(un)) return talukToDistrict[key];
        if (regex.test(sec)) return talukToDistrict[key];
    }

    if (un.includes('belagavi city')) return 'Belagavi City';
    if (un.includes('bengaluru city')) return 'Bengaluru City';
    if (un.includes('hubballi') || un.includes('dharwad')) return 'Hubballi-Dharwad City';
    if (un.includes('mangaluru city')) return 'Mangaluru City';
    if (un.includes('mysuru city')) return 'Mysuru City';
    if (un.includes('kalaburagi city')) return 'Kalaburagi City';

    for (const d of districts) {
        if (un.includes(d.toLowerCase())) return d;
    }

    // 5. Fallback range headquarter districts for range units
    if (unitRangeKey) {
        if (unitRangeKey === 'cr') return 'Bengaluru Rural';
        if (unitRangeKey === 'er') return 'Davanagere';
        if (unitRangeKey === 'nr') return 'Belagavi';
        if (unitRangeKey === 'sr') return 'Mysuru';
        if (unitRangeKey === 'wr') return 'Dakshina Kannada';
        if (unitRangeKey === 'ner') return 'Kalaburagi';
        if (unitRangeKey === 'br') return 'Ballari';
    }

    return 'Statelevel';
}

async function startSync() {
    console.log('🔄 Loading datasets...');
    const webContacts = parseCSV(csvPath);
    const dbContacts = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));

    console.log(`📊 Loaded ${webContacts.length} contacts from Website CSV.`);
    console.log(`📊 Loaded ${dbContacts.length} contacts from Firestore JSON.`);

    // Map Firestore contacts for lookup using arrays to handle duplicate names
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

        if (r.email && r.email.trim()) {
            addToMapArray(dbByEmail, r.email, r);
        }
        if (r.email2 && r.email2.trim()) {
            addToMapArray(dbByEmail, r.email2, r);
        }
        
        const comp3 = `${name}|${unit}|${subDivision}`;
        const comp2 = `${name}|${unit}`;
        
        addToMapArray(dbByCompound3, comp3, r);
        addToMapArray(dbByCompound2, comp2, r);
        addToMapArray(dbByName, name, r);
    });

    // Find next AGID sequence number
    let nextAgidNum = 3001; 
    const agidNums = dbContacts
        .map(r => r.agid)
        .filter(a => String(a).startsWith('AGID'))
        .map(a => parseInt(a.replace('AGID', '')))
        .filter(n => !isNaN(n));
    if (agidNums.length > 0) {
        const maxReal = Math.max(...agidNums);
        if (maxReal >= nextAgidNum) nextAgidNum = maxReal + 1;
    }

    console.log(`👉 Next generated AGID sequential suffix starts at: ${nextAgidNum}`);

    const syncedRecords = [];
    const matchedDocs = new Set(); // Track docIds we matched

    let matchedByEmailCount = 0;
    let matchedByDesignationCount = 0;
    let insertedCount = 0;

    webContacts.forEach(web => {
        const cleanName = normalizeDesignationName(web.Designation);
        const webEmail = (web.Email || '').split(',')[0].trim().toLowerCase();
        const webDesig = (web.Designation || '').trim().toLowerCase();

        let matched = null;

        const findMatch = () => {
            // 1. Try Email
            if (webEmail) {
                const candidates = dbByEmail.get(webEmail) || [];
                const found = candidates.find(c => !matchedDocs.has(c.docId));
                if (found) {
                    matchedByEmailCount++;
                    return found;
                }
            }

            // 2. Try Compound 3: Name + Unit + Section
            const c3Key = `${webDesig}|${shortenRange(web.Unit).toLowerCase()}|${web.Section.toLowerCase()}`;
            const candidates3 = dbByCompound3.get(c3Key) || [];
            const found3 = candidates3.find(c => !matchedDocs.has(c.docId));
            if (found3) {
                matchedByDesignationCount++;
                return found3;
            }

            // 3. Try Compound 2: Name + Unit
            const c2Key = `${webDesig}|${shortenRange(web.Unit).toLowerCase()}`;
            const candidates2 = dbByCompound2.get(c2Key) || [];
            const found2 = candidates2.find(c => !matchedDocs.has(c.docId));
            if (found2) {
                matchedByDesignationCount++;
                return found2;
            }

            // 4. Try Name
            const candidates1 = dbByName.get(webDesig) || [];
            const found1 = candidates1.find(c => !matchedDocs.has(c.docId));
            if (found1) {
                matchedByDesignationCount++;
                return found1;
            }

            return null;
        };

        matched = findMatch();

        const phoneInfo = parsePhone(web.Phone);
        const emailInfo = parseEmail(web.Email);

        let record = {};

        if (matched) {
            matchedDocs.add(matched.docId);
            
            // Normalize rank from designation to match DIRECTORY_ORGANIZATION_RULES.md
            const rank = normalizeRank(extractRank(web.Designation));
            
            // Retain district if it already exists and is valid
            const district = getDistrict(web.Designation, web.Unit, web.Section, matched.district, web.Email);

            record = {
                docId: matched.docId,
                agid: matched.agid,
                name: cleanName,
                rank: rank,
                office: cleanName,
                station: cleanName,
                unit: shortenRange(web.Unit),
                subDivision: web.Section,
                district: district,
                landline: phoneInfo.landline,
                landline2: phoneInfo.landline2,
                mobile: phoneInfo.mobile,
                mobile2: phoneInfo.mobile2,
                email: emailInfo.email,
                email2: emailInfo.email2,
                photoUrl: matched.photoUrl || null,
                bloodGroup: matched.bloodGroup || null,
                isHidden: matched.isHidden || false,
                createdAt: matched.createdAt || new Date().toISOString(),
                updatedAt: new Date().toISOString()
            };
        } else {
            // New record insertion
            const agid = `AGID${String(nextAgidNum).padStart(4, '0')}`;
            nextAgidNum++;
            insertedCount++;

            const rank = normalizeRank(extractRank(web.Designation));
            const district = getDistrict(web.Designation, web.Unit, web.Section, null, web.Email);

            record = {
                docId: agid,
                agid: agid,
                name: cleanName,
                rank: rank,
                office: cleanName,
                station: cleanName,
                unit: shortenRange(web.Unit),
                subDivision: web.Section,
                district: district,
                landline: phoneInfo.landline,
                landline2: phoneInfo.landline2,
                mobile: phoneInfo.mobile,
                mobile2: phoneInfo.mobile2,
                email: emailInfo.email,
                email2: emailInfo.email2,
                photoUrl: null,
                bloodGroup: null,
                isHidden: false,
                createdAt: new Date().toISOString(),
                updatedAt: new Date().toISOString()
            };
        }

        // Generate Search Blob
        const blobParts = [
            record.agid, record.name, record.rank, record.office, record.station,
            record.unit, record.district, record.subDivision,
            record.landline, record.landline2, record.mobile, record.mobile2,
            record.email, record.email2
        ].filter(Boolean).map(v => String(v).toLowerCase().trim());
        record.searchBlob = [...new Set(blobParts)].join(' ');

        syncedRecords.push(record);
    });

    // Identify stale documents to delete
    const staleDocs = dbContacts.filter(r => !matchedDocs.has(r.docId));

    console.log('\n════════════════════════════════════════════════');
    console.log('📊 DRY RUN REPORT');
    console.log('════════════════════════════════════════════════');
    console.log(`✅ Matches found by Email:        ${matchedByEmailCount}`);
    console.log(`✅ Matches found by Designation:  ${matchedByDesignationCount}`);
    console.log(`🆕 New records to insert:         ${insertedCount}`);
    console.log(`🗑️  Stale records to delete:       ${staleDocs.length}`);
    console.log(`👉 Total records to write:        ${syncedRecords.length}`);
    console.log('════════════════════════════════════════════════\n');

    if (commitChanges) {
        console.log('🚀 COMMITTING CHANGES TO FIRESTORE...');

        // 1. Process Updates & Inserts in batches of 500
        let batch = db.batch();
        let count = 0;

        for (const record of syncedRecords) {
            const docRef = collectionRef.doc(record.docId);
            
            // Remove docId from data object itself
            const { docId, ...dataToSave } = record;
            
            // Convert native JS dates/strings if necessary
            batch.set(docRef, dataToSave);
            count++;

            if (count % 500 === 0) {
                await batch.commit();
                console.log(`Committed ${count} updates/inserts...`);
                batch = db.batch();
            }
        }
        if (count % 500 !== 0) {
            await batch.commit();
            console.log(`Committed final ${count} updates/inserts.`);
        }

        // 2. Process Deletions in batches of 500
        batch = db.batch();
        let deleteCount = 0;
        for (const stale of staleDocs) {
            const docRef = collectionRef.doc(stale.docId);
            batch.delete(docRef);
            deleteCount++;

            if (deleteCount % 500 === 0) {
                await batch.commit();
                console.log(`Deleted ${deleteCount} stale records...`);
                batch = db.batch();
            }
        }
        if (deleteCount % 500 !== 0) {
            await batch.commit();
            console.log(`Deleted final ${deleteCount} stale records.`);
        }

        console.log('🎉 --- DATABASE SMART SYNC COMPLETE ---');
    } else {
        console.log('💡 Dry run completed. Use command parameter "--commit" to apply changes.');
    }

    process.exit(0);
}

startSync().catch(err => {
    console.error('❌ Sync failed:', err);
    process.exit(1);
});
