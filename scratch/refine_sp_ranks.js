const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);

// Define mapping for the specific rank fixes requested by user
const rankMap = [
    { regex: /^DSP$|Deputy\s*SP/i, replace: "DySP" },
    { regex: /Assistant\s*SP|^ASP$/i, replace: "ASP" },
    { regex: /^ADDL_SP$|Additional\s*SP|Addl\.?\s*SP/i, replace: "Addl.SP" }
];

const newWb = xlsx.utils.book_new();

wbSource.SheetNames.forEach(sheetName => {
    const data = xlsx.utils.sheet_to_json(wbSource.Sheets[sheetName]);
    
    data.forEach(row => {
        if (row.Rank) {
            let originalRank = String(row.Rank);
            
            // First fix RETD prefix if exists
            let isRetd = /^RETD\.\s*|^Retired\s*/i.test(originalRank);
            if (isRetd) {
                originalRank = originalRank.replace(/^RETD\.\s*|^Retired\s*/i, '').trim();
            }

            let newRank = originalRank;
            
            // Apply mappings
            for (let mapping of rankMap) {
                if (mapping.regex.test(newRank)) {
                    newRank = newRank.replace(mapping.regex, mapping.replace).trim();
                    break; 
                }
            }
            
            // Add back RETD if needed
            if (isRetd) {
                newRank = "RETD. " + newRank;
            }
            
            row.Rank = newRank;
        }
    });

    const ws = xlsx.utils.json_to_sheet(data);
    xlsx.utils.book_append_sheet(newWb, ws, sheetName);
});

xlsx.writeFile(newWb, masterPath);
console.log("Specific rank updates (DySP, ASP, Addl.SP) successfully applied to all sheets.");
