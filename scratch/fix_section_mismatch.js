const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);
const data = xlsx.utils.sheet_to_json(wbSource.Sheets['MASTER_MERGED_FINAL']);

// List of units that should ALWAYS get their own functional sheet
const functionalUnits = [
    "KSRP", "ISD", "Intelligence", "CEN", "DCRE", "FSL", "CID",
    "ASC Team", "BDDS", "Control Room", "CCB", "CCRB", "CDR", "Coast Guard", "Computer", 
    "Court", "CSB", "CSP", "DCIB", "DCRB", "Dog Squad", "DSB", "ERSS", "ESCOM", 
    "Excise", "Fire", "Forest", "FPB", "FRRO", "Guest House", "Health", "Home Guard", 
    "Lokayukta", "Prison", "Railway", "RTO", "SCRB", "Social Media", "Toll", "Traffic", "VVIP", "Wireless", "Training"
];

// Re-process data from Master
const sheetsData = { "MASTER_MERGED_FINAL": [] };

data.forEach(row => {
    // Fix Section data mismatch: Align Section with UNIT if they are misaligned for functional units
    if (functionalUnits.includes(row.UNIT)) {
        row.Section = row.UNIT;
    } else if (row.Section === "State INT") {
        row.Section = "Intelligence";
    }

    let targetSheet = "General";
    
    // Strict priority: Functional Units ALWAYS go to their functional sheet, not a regional sheet
    if (functionalUnits.includes(row.UNIT)) {
        targetSheet = row.UNIT;
    } else {
        // If it's a regular officer (L&O, Admin, Others), use the Range/City mapping
        targetSheet = row.Range || row.District || row.UNIT || "General";
    }
    
    // Clean target sheet name
    targetSheet = targetSheet.substring(0, 31).replace(/[\\\/\?\*\[\]\:]/g, "");
    
    if (!sheetsData[targetSheet]) sheetsData[targetSheet] = [];
    sheetsData[targetSheet].push(row);
    sheetsData["MASTER_MERGED_FINAL"].push(row); // Keep master populated
});

// Create new workbook
const newWb = xlsx.utils.book_new();

// Append Master first
const wsMaster = xlsx.utils.json_to_sheet(sheetsData["MASTER_MERGED_FINAL"], {
    header: ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"]
});
xlsx.utils.book_append_sheet(newWb, wsMaster, "MASTER_MERGED_FINAL");

// Append the rest alphabetically
Object.keys(sheetsData).sort().forEach(name => {
    if (name !== "MASTER_MERGED_FINAL") {
        const ws = xlsx.utils.json_to_sheet(sheetsData[name], {
            header: ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"]
        });
        xlsx.utils.book_append_sheet(newWb, ws, name);
    }
});

xlsx.writeFile(newWb, masterPath);
console.log(`Functional separation complete. Created ${Object.keys(sheetsData).length} exact sheets.`);
