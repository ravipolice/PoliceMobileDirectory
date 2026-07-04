const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);
// Get data from the master sheet (assuming it's named MASTER_MERGED_FINAL)
const rawData = xlsx.utils.sheet_to_json(wbSource.Sheets['MASTER_MERGED_FINAL']);

const rangeMap = {
    "Bagalkote": "Northern Range – Belagavi", "Belagavi": "Northern Range – Belagavi",
    "Dharwad": "Northern Range – Belagavi", "Gadag": "Northern Range – Belagavi", 
    "Vijayapura": "Northern Range – Belagavi",
    "Ballari": "Ballari Range – Ballari", "Raichur": "Ballari Range – Ballari", 
    "Koppal": "Ballari Range – Ballari", "Vijayanagara": "Ballari Range – Ballari",
    "Bidar": "North-Eastern Range – Kalaburag", "Kalaburagi": "North-Eastern Range – Kalaburag", 
    "Yadgir": "North-Eastern Range – Kalaburag",
    "Chamarajanagara": "Southern Range – Mysuru", "Hassan": "Southern Range – Mysuru", 
    "Kodagu": "Southern Range – Mysuru", "Mandya": "Southern Range – Mysuru", 
    "Mysuru": "Southern Range – Mysuru",
    "Chikkaballapura": "Central Range – Bengaluru", "Kolar": "Central Range – Bengaluru", 
    "Ramanagara": "Central Range – Bengaluru", "Tumakuru": "Central Range – Bengaluru",
    "Bengaluru Rural": "Central Range – Bengaluru",
    "Chitradurga": "Eastern Range – Davanagere", "Davanagere": "Eastern Range – Davanagere", 
    "Haveri": "Eastern Range – Davanagere",
    "Dakshina Kannada": "Western Range – Mangaluru", "Mangaluru": "Western Range – Mangaluru",
    "Udupi": "Western Range – Mangaluru", "Chikkamagaluru": "Western Range – Mangaluru", 
    "Shivamogga": "Western Range – Mangaluru", "Uttara Kannada": "Western Range – Mangaluru"
};

const specialCities = ["Bengaluru City", "Belagavi City", "Hubballi–Dharwad City", "Mangaluru City", "Mysuru City", "Kalaburagi City"];

const restructuredData = [];

rawData.forEach(r => {
    let dist = r.District || "";
    let unit = r.Unit || "";
    
    // Determine Range
    let assignedRange = "";
    if (specialCities.includes(dist)) {
        assignedRange = dist;
    } else if (rangeMap[dist]) {
        assignedRange = rangeMap[dist];
    } else {
        assignedRange = unit || "State Level";
    }

    // Map to new exact format
    restructuredData.push({
        "UNIT": unit,
        "Range": assignedRange,
        "District": dist,
        "Section": r.Section || "",
        "Name": r.Name || "",
        "Rank": r.Rank || "",
        "station": r.Station || r.station || "",
        "office1": r['Office 1'] || r.office1 || "",
        "office 2": r['Office 2'] || r['office 2'] || "",
        "mobile 1": r['Mobile 1'] || r['mobile 1'] || "",
        "mobile 2": r['Mobile 2'] || r['mobile 2'] || "",
        "email1": r.Email || r.email1 || "",
        "email2": r.email2 || "",
        "agid": r.agid || ""
    });
});

// Create new workbook
const newWb = xlsx.utils.book_new();

// 1. MASTER SHEET FIRST
const sheetsData = { "MASTER_MERGED_FINAL": restructuredData };

// 2. Split into categories
restructuredData.forEach(row => {
    let target = row.Range;
    target = target.substring(0, 31).replace(/[\\\/\?\*\[\]\:]/g, "");
    
    if (!sheetsData[target]) sheetsData[target] = [];
    sheetsData[target].push(row);
});

// Append Master first
const wsMaster = xlsx.utils.json_to_sheet(sheetsData["MASTER_MERGED_FINAL"]);
xlsx.utils.book_append_sheet(newWb, wsMaster, "MASTER_MERGED_FINAL");

// Append the rest alphabetically
Object.keys(sheetsData).sort().forEach(name => {
    if (name !== "MASTER_MERGED_FINAL") {
        const ws = xlsx.utils.json_to_sheet(sheetsData[name]);
        xlsx.utils.book_append_sheet(newWb, ws, name);
    }
});

xlsx.writeFile(newWb, masterPath);
console.log(`Reorganization complete. Master is first. Created ${Object.keys(sheetsData).length} total sheets.`);
