const xlsx = require('xlsx');
const path = require('path');

const csvPath = path.join(__dirname, '..', 'KSP_Officers_App.csv');
const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');

// 1. Load CSV and create AGID -> District map
const wbCsv = xlsx.readFile(csvPath);
const csvData = xlsx.utils.sheet_to_json(wbCsv.Sheets[wbCsv.SheetNames[0]]);
const agidMap = {};
csvData.forEach(r => {
    if (r.agid && r.district) agidMap[r.agid] = r.district;
});

// 2. Load Excel Master
const wbSource = xlsx.readFile(masterPath);
const data = xlsx.utils.sheet_to_json(wbSource.Sheets['MASTER_MERGED']);

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
    "Ramanagara": "Central Range – Bengaluru", "Tumakuru": "Central Range – Bengaluru", "Bengaluru Rural": "Central Range – Bengaluru",
    "Chitradurga": "Eastern Range – Davanagere", "Davanagere": "Eastern Range – Davanagere", 
    "Haveri": "Eastern Range – Davanagere",
    "Dakshina Kannada": "Western Range – Mangaluru", "Mangaluru": "Western Range – Mangaluru",
    "Udupi": "Western Range – Mangaluru", "Chikkamagaluru": "Western Range – Mangaluru", 
    "Shivamogga": "Western Range – Mangaluru", "Uttara Kannada": "Western Range – Mangaluru"
};

const specialCities = ["Bengaluru City", "Belagavi City", "Hubballi–Dharwad City", "Mangaluru City", "Mysuru City", "Kalaburagi City"];

const newWb = xlsx.utils.book_new();
const sheetsData = { "MASTER_MERGED": [] };

data.forEach(row => {
    // Priority 1: Map by AGID from CSV
    if (agidMap[row.agid]) {
        row.District = agidMap[row.agid];
    }
    
    const dist = row.District || "";
    const unit = row.Unit || "";

    let target = "General";
    if (specialCities.includes(dist)) target = dist;
    else if (rangeMap[dist]) target = rangeMap[dist];
    else if (unit) target = unit;

    target = target.substring(0, 31).replace(/[\\\/\?\*\[\]\:]/g, "");
    if (!sheetsData[target]) sheetsData[target] = [];
    
    sheetsData[target].push(row);
    sheetsData["MASTER_MERGED"].push(row);
});

Object.keys(sheetsData).sort().forEach(name => {
    const ws = xlsx.utils.json_to_sheet(sheetsData[name]);
    xlsx.utils.book_append_sheet(newWb, ws, name);
});

xlsx.writeFile(newWb, masterPath);
console.log(`AGID Rebuild complete with ${Object.keys(sheetsData).length} sheets.`);
