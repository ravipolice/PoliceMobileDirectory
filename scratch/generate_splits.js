const xlsx = require('xlsx');
const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const data = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED']);

const rangeMap = {
    "Bagalkot": "Northern Range – Belagavi", "Bagalkote": "Northern Range – Belagavi",
    "Belagavi": "Northern Range – Belagavi", "Dharwad": "Northern Range – Belagavi",
    "Gadag": "Northern Range – Belagavi", "Vijayapura": "Northern Range – Belagavi",
    "Ballari": "Ballari Range – Ballari", "Raichur": "Ballari Range – Ballari", 
    "Koppal": "Ballari Range – Ballari", "Vijayanagara": "Ballari Range – Ballari",
    "Bidar": "North-Eastern Range – Kalaburag", "Kalaburagi": "North-Eastern Range – Kalaburag",
    "Yadgir": "North-Eastern Range – Kalaburag",
    "Chamarajanagar": "Southern Range – Mysuru", "Chamarajanagara": "Southern Range – Mysuru",
    "Hassan": "Southern Range – Mysuru", "Kodagu": "Southern Range – Mysuru", 
    "Mandya": "Southern Range – Mysuru", "Mysuru": "Southern Range – Mysuru",
    "Chikkaballapura": "Central Range – Bengaluru", "Kolar": "Central Range – Bengaluru", 
    "Ramanagara": "Central Range – Bengaluru", "Tumakuru": "Central Range – Bengaluru",
    "Chitradurga": "Eastern Range – Davanagere", "Davanagere": "Eastern Range – Davanagere", 
    "Haveri": "Eastern Range – Davanagere",
    "Dakshina Kannada": "Western Range – Mangaluru", "Mangaluru": "Western Range – Mangaluru",
    "Udupi": "Western Range – Mangaluru", "Chikkamagaluru": "Western Range – Mangaluru", 
    "Shivamogga": "Western Range – Mangaluru", "Uttara Kannada": "Western Range – Mangaluru"
};

const specialCities = ["Bengaluru City", "Belagavi City", "Hubballi-Dharwad City", "Hubballi–Dharwad City", "Mangaluru City", "Mysuru City", "Kalaburagi City"];

const splits = {};

data.forEach(row => {
    let target = "General";
    const dist = row.District || "";
    const unit = row.Unit || "";
    
    if (specialCities.includes(dist)) target = dist;
    else if (rangeMap[dist]) target = rangeMap[dist];
    else if (unit) target = unit;
    
    // Normalize target name (limit 31 chars, remove invalid chars)
    target = target.substring(0, 31).replace(/[\\\/\?\*\[\]\:]/g, "");
    
    if (!splits[target]) splits[target] = [];
    splits[target].push(row);
});

// Save each split to a temporary CSV
const tempDir = path.join(__dirname, 'temp_splits');
if (!fs.existsSync(tempDir)) fs.mkdirSync(tempDir);

Object.keys(splits).forEach(target => {
    const ws = xlsx.utils.json_to_sheet(splits[target]);
    const csv = xlsx.utils.sheet_to_csv(ws);
    fs.writeFileSync(path.join(tempDir, `${target}.csv`), csv);
});

console.log(`Generated ${Object.keys(splits).length} CSV splits.`);
