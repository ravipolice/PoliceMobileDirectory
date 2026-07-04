const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);

const validRanges = [
    "Central Range", "Northern Range", "North Eastern Range", "North-Eastern Range", 
    "Ballari Range", "Southern Range", "Western Range", "Davangere Range", "Eastern Range"
];

const validCommissionerates = [
    "Bengaluru City", "Hubballi Dharwad City", "Hubballi-Dharwad City", "Mysuru City", 
    "Mangaluru City", "Belagavi City", "Kalaburagi City"
];

const newWb = xlsx.utils.book_new();

wbSource.SheetNames.forEach(sheetName => {
    const data = xlsx.utils.sheet_to_json(wbSource.Sheets[sheetName]);
    
    data.forEach(row => {
        if (row.Range) {
            let currentRange = String(row.Range).trim();
            let matched = false;
            
            // Check if it contains a valid Range
            for (let valid of validRanges) {
                // E.g., "Western Range - Mangaluru" will match "Western Range"
                if (currentRange.toLowerCase().includes(valid.toLowerCase())) {
                    row.Range = valid;
                    matched = true;
                    break;
                }
            }
            
            // Check if it contains a valid Commissionerate
            if (!matched) {
                for (let valid of validCommissionerates) {
                    if (currentRange.toLowerCase().includes(valid.toLowerCase())) {
                        row.Range = valid;
                        matched = true;
                        break;
                    }
                }
            }
            
            // If it's something else like "State Level", clear it out
            if (!matched) {
                row.Range = "";
            }
        }
    });

    const ws = xlsx.utils.json_to_sheet(data, {
        header: ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"]
    });
    xlsx.utils.book_append_sheet(newWb, ws, sheetName);
});

xlsx.writeFile(newWb, masterPath);
console.log("Range column strictly filtered to only contain valid Ranges and Commissionerates.");
