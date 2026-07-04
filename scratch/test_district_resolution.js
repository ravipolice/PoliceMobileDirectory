const fs = require('fs');
const path = require('path');

const csvPath = path.join(__dirname, '..', 'KSP_Contacts_Master_Clean.csv');
const jsonPath = path.join(__dirname, '..', 'officers_real_db.json');

const content = fs.readFileSync(csvPath, 'utf8');
const lines = content.split(/\r?\n/);
const webContacts = [];
const headerLine = lines[0].replace(/^\uFEFF/, '');
const cleanHeaders = headerLine.split(',').map(h => h.replace(/^"|"$/g, '').trim());

for (let i = 1; i < lines.length; i++) {
    const line = lines[i].trim();
    if (!line) continue;
    let matches = [];
    let insideQuote = false;
    let entry = '';
    for (let j = 0; j < line.length; j++) {
        const char = line[j];
        if (char === '"') insideQuote = !insideQuote;
        else if (char === ',' && !insideQuote) {
            matches.push(entry.trim());
            entry = '';
        } else entry += char;
    }
    matches.push(entry.trim());
    const record = {};
    cleanHeaders.forEach((header, index) => {
        record[header] = matches[index] ? matches[index].replace(/^"|"$/g, '').trim() : '';
    });
    webContacts.push(record);
}

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
    'Koppal', 'Mandya', 'Mysuru', 'Raichur', 'Ramanagara', 'Shivamogga', 'Tumakuru', 
    'Udupi', 'Uttara Kannada', 'Vijayapura', 'Yadgiri', 'Koppala', 'Hubballi-Dharwad City', 'Mangaluru City', 'Belagavi City', 'Kalaburagi City'
];

districts.forEach(d => {
    if (!talukToDistrict[d]) talukToDistrict[d] = d;
});
const talukKeys = Object.keys(talukToDistrict).sort((a, b) => b.length - a.length);

function getDistrict(designation, unit, section, fallbackDistrict) {
    if (fallbackDistrict) {
        const cleaned = fallbackDistrict.trim();
        const isValid = districts.some(d => d.toLowerCase() === cleaned.toLowerCase()) ||
                        cleaned.toLowerCase() === 'statelevel' ||
                        cleaned.toLowerCase() === 'state level';
        if (isValid) {
            return cleaned;
        }
    }
    
    const des = (designation || '').toLowerCase();
    const un = (unit || '').toLowerCase();
    const sec = (section || '').toLowerCase();
    
    for (const key of talukKeys) {
        const regex = new RegExp('\\b' + key.toLowerCase() + '\\b', 'i');
        if (regex.test(des)) return talukToDistrict[key];
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
    
    return 'Statelevel';
}

const dbContacts = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
const dbByEmail = new Map();
const dbByName = new Map();

dbContacts.forEach(r => {
    if (r.email && r.email.trim()) dbByEmail.set(r.email.trim().toLowerCase(), r);
    const nameKey = (r.name || '').trim().toLowerCase();
    if (nameKey) dbByName.set(nameKey, r);
});

let diffCount = 0;
webContacts.forEach(web => {
    const webEmail = (web.Email || '').split(',')[0].trim().toLowerCase();
    const webDesig = (web.Designation || '').trim().toLowerCase();
    let matched = null;
    if (webEmail && dbByEmail.has(webEmail)) matched = dbByEmail.get(webEmail);
    else if (webDesig && dbByName.has(webDesig)) matched = dbByName.get(webDesig);

    if (matched) {
        const newDist = getDistrict(web.Designation, web.Unit, web.Section, matched.district);
        if (newDist !== matched.district) {
            diffCount++;
            if (diffCount <= 20) {
                console.log(`Match: ${web.Designation} | Unit: ${web.Unit} | Section: ${web.Section}`);
                console.log(`   Old District: ${matched.district} -> New District: ${newDist}`);
            }
        }
    }
});
console.log(`\nTotal districts changing: ${diffCount}`);
