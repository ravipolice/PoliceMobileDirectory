const fs = require('fs');
const path = require('path');
const xlsx = require('xlsx');

const csvPath = path.join(__dirname, '..', 'KSP_Contacts_Master_Clean.csv');
const csvContent = fs.readFileSync(csvPath, 'utf8');
const csvData = xlsx.utils.sheet_to_json(xlsx.read(csvContent, { type: 'string' }).Sheets.Sheet1);

const karnatakaDistricts = [
    "Bengaluru Rural", "Bengaluru Urban", "Chikkaballapura", "Kolar", "Ramanagara", "Tumakuru",
    "Mysuru", "Chamarajanagar", "Mandya", "Hassan", "Kodagu",
    "Dakshina Kannada", "Udupi", "Chikkamagaluru", "Uttara Kannada", "Shivamogga",
    "Davanagere", "Davangere", "Chitradurga", "Haveri",
    "Belagavi", "Belgaum", "Bagalkote", "Bagalkot", "Vijayapura", "Bijapur", "Dharwad", "Gadag",
    "Kalaburagi", "Gulbarga", "Bidar", "Yadgir",
    "Ballari", "Bellary", "Koppal", "Raichur", "Vijayanagara"
];

const districtAliases = {
    "Bellary": "Ballari", "Bellari": "Ballari",
    "Belgaum": "Belagavi", "Bela": "Belagavi",
    "Gulbarga": "Kalaburagi", "Kalburgi": "Kalaburagi",
    "Bangalore": "Bengaluru Rural",
    "Mysore": "Mysuru",
    "Mangalore": "Dakshina Kannada",
    "Hubli": "Dharwad", "Dharwad": "Dharwad",
    "Shimoga": "Shivamogga", "Bijapur": "Vijayapura",
    "Chikmagalur": "Chikkamagaluru", "Tumkur": "Tumakuru",
    "Vijayanagar": "Vijayanagara"
};

// We will replicate the getUnitShortCode and other helpers to match
const rankMapping = { 'AO': 'AO', 'AAO': 'AAO' };
const unitMapping = { 'Range': 'L&O', 'Commissionerate': 'L&O' };

function getUnitShortCode(rawUnit) {
    if (!rawUnit) return "Others";
    let u = String(rawUnit).trim();
    u = u.replace(/^(ADGP|IGP|DIG|SP|DySP|PI|PSI|PA|AAO|SS|FDA|SDA|STENO|TYPIST)[,\s]+/i, "").trim();
    if (u.includes("L&O") || u.includes("L & O") || u.includes("Law & Order") || u.includes("Law and Order")) return "L&O";
    const sortedKeys = Object.keys(unitMapping).sort((a, b) => b.length - a.length);
    for (const key of sortedKeys) {
        if (u.toUpperCase().includes(key.toUpperCase())) return unitMapping[key];
    }
    return u;
}

let currentRange = "";
let currentDistrict = "";

csvData.forEach((r, idx) => {
    const rawCol1 = String(r.Section || "").trim();
    const rawCol2 = String(r.Unit || "").trim();
    const des = String(r.Designation || "").trim();
    
    // Trace line 3619 (which is index 3617)
    if (idx === 3617) {
        console.log(`Trace for Line ${idx + 2}:`);
        console.log(`rawCol1: ${rawCol1}, rawCol2: ${rawCol2}, des: ${des}`);
        
        let unit = getUnitShortCode(rawCol2 || rawCol1);
        console.log(`Initial unit: ${unit}`);
        
        if (rawCol1 === "Ranges" || rawCol1.includes("Range")) {
            if (rawCol2 !== currentRange) {
                console.log(`Range changed from ${currentRange} to ${rawCol2}`);
            }
        }
    }
    
    // Keep track of current district in Ranges section (simulated)
    if (rawCol1 === "Ranges" || rawCol1.includes("Range")) {
        if (rawCol2 !== currentRange) {
            currentRange = rawCol2;
            currentDistrict = "";
        }
        const desUpper = des.toUpperCase();
        for (const dist of karnatakaDistricts) {
            const distUpper = dist.toUpperCase();
            if (desUpper.includes(`SP, ${distUpper}`) || 
                desUpper.includes(`SP ${distUpper}`) || 
                desUpper.includes(`SUPERINTENDENT OF POLICE, ${distUpper}`) ||
                (desUpper.includes("ADDL. SP") && desUpper.includes(distUpper))
            ) {
                currentDistrict = districtAliases[dist] || dist;
                if (idx > 3610 && idx < 3625) {
                    console.log(`Index ${idx}: Found SP of ${currentDistrict}`);
                }
                break;
            }
        }
    }
});
