const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V2.xlsx');
const wbSource = xlsx.readFile(masterPath);
const data = xlsx.utils.sheet_to_json(wbSource.Sheets[wbSource.SheetNames[0]]);

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

const newWb = xlsx.utils.book_new();

// 1. Create Master Merged
const wsMaster = xlsx.utils.json_to_sheet(data);
xlsx.utils.book_append_sheet(newWb, wsMaster, "MASTER_MERGED");

// 2. Group data for other sheets
const sheetsData = {};
data.forEach(row => {
    let target = "General";
    const dist = row.District || "";
    const unit = row.Unit || "";
    
    if (specialCities.includes(dist)) target = dist;
    else if (rangeMap[dist]) target = rangeMap[dist];
    else if (unit) target = unit;
    
    target = target.substring(0, 31).replace(/[\\\/\?\*\[\]\:]/g, "");
    if (!sheetsData[target]) sheetsData[target] = [];
    sheetsData[target].push(row);
});

// 3. Append sheets in alphabetical order (optional)
Object.keys(sheetsData).sort().forEach(name => {
    if (name === "MASTER_MERGED") return;
    const ws = xlsx.utils.json_to_sheet(sheetsData[name]);
    xlsx.utils.book_append_sheet(newWb, ws, name);
});

// 4. Save to new file
const finalPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3_RESTORED.xlsx');
xlsx.writeFile(newWb, finalPath);

console.log(`Successfully created workbook with ${Object.keys(sheetsData).length + 1} sheets.`);
