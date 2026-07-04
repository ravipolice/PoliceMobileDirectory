const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);
const rawData = xlsx.utils.sheet_to_json(wbSource.Sheets['MASTER_MERGED_FINAL']);

// Exact list from Android App
const validUnits = [
    "L&O", "DAR", "CAR", "Ministerial", "KSRP", "ISD", "Intelligence", "CEN", "DCRE", "FSL", "CID",
    "Admin", "ASC Team", "BDDS", "Control Room", "CCB", "CCRB", "CDR", "Coast Guard", "Computer", 
    "Court", "CSB", "CSP", "DCIB", "DCRB", "Dog Squad", "DSB", "ERSS", "ESCOM", 
    "Excise", "Fire", "Forest", "FPB", "FRRO", "Guest House", "Health", "Home Guard", 
    "IPS", "Lokayukta", "Others", "Prison", 
    "Railway", "RTO", "SCRB", "Social Media", "Toll", "Traffic", "VVIP", "Wireless", "Training"
];

// Normalization mapping
const unitMap = {
    "Law & Order": "L&O",
    "Administration": "Admin",
    "C/Room": "Control Room",
    "State INT": "Intelligence",
    "Prisons": "Prison",
    "Railways": "Railway",
    "Forest Cell": "Forest",
    "HG & CD": "Home Guard",
    "G&HR": "Admin",
    "C&TS": "Computer",
    "KSISF": "KSRP", // Rolling into KSRP or Others depending on preference, mapping to KSRP for now
    "ANTF": "CID", 
    "SIT": "CID",
    "Special Task Force (STF)": "Others",
    "SPORTI": "Training",
    "Sports Board": "Others",
    "KSPH & IDCL": "Others",
    "IRB": "KSRP",
    "Retired": "Others",
    "On Deputation": "Others"
};

const restructuredData = [];

rawData.forEach(r => {
    let oldUnit = String(r.UNIT || r.Unit || "").trim();
    
    // Normalize Unit
    let newUnit = unitMap[oldUnit] || oldUnit;
    
    // If it's completely unknown and not empty, mark as Others
    if (newUnit && !validUnits.includes(newUnit)) {
        // Fallback checks
        if (newUnit.includes("Traffic")) newUnit = "Traffic";
        else if (newUnit.includes("Women")) newUnit = "L&O";
        else newUnit = "Others"; 
    }
    
    // Overwrite the field
    r.UNIT = newUnit;
    r.Unit = newUnit; // Ensure backward compat if script relies on it
    
    // If the Range was set to the old unit name, update it
    if (r.Range === oldUnit && oldUnit !== "") {
        r.Range = newUnit;
    }

    restructuredData.push(r);
});

// Re-build workbook
const newWb = xlsx.utils.book_new();
const sheetsData = { "MASTER_MERGED_FINAL": restructuredData };

restructuredData.forEach(row => {
    let target = row.Range;
    if (!target || target === "State Level") target = row.UNIT || "General";
    
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
console.log(`Unit normalization complete. Mapped to app constants. Created ${Object.keys(sheetsData).length} sheets.`);
