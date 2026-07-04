const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);

const newWb = xlsx.utils.book_new();

wbSource.SheetNames.forEach(sheetName => {
    const data = xlsx.utils.sheet_to_json(wbSource.Sheets[sheetName]);
    
    data.forEach(row => {
        if (row.Rank) {
            let originalRank = String(row.Rank);
            
            // Handle "RETD. " prefix
            let isRetd = /^RETD\.\s*|^Retired\s*/i.test(originalRank);
            if (isRetd) {
                originalRank = originalRank.replace(/^RETD\.\s*|^Retired\s*/i, '').trim();
            }

            // Split by comma or hyphen to remove trailing unit/station data
            // E.g., "PI, Finger Print Bureau" -> "PI"
            let cleanRank = originalRank.split(',')[0].trim();
            cleanRank = cleanRank.split('-')[0].trim();
            
            // Remove any trailing " in", " at", etc if they slipped through
            cleanRank = cleanRank.replace(/\s+(in|at|of)\s+.*$/i, '').trim();

            if (isRetd) {
                cleanRank = "RETD. " + cleanRank;
            }
            
            row.Rank = cleanRank;
        }
    });

    const ws = xlsx.utils.json_to_sheet(data, {
        header: ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"]
    });
    xlsx.utils.book_append_sheet(newWb, ws, sheetName);
});

xlsx.writeFile(newWb, masterPath);
console.log("Rank column strictly cleaned to contain only designations.");
