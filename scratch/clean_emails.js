const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);

const newWb = xlsx.utils.book_new();

// Regex to strictly extract a valid email address
const emailRegex = /[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/;

wbSource.SheetNames.forEach(sheetName => {
    const data = xlsx.utils.sheet_to_json(wbSource.Sheets[sheetName]);
    
    data.forEach(row => {
        // Clean email1
        if (row.email1) {
            const str = String(row.email1);
            const match = str.match(emailRegex);
            if (match) {
                row.email1 = match[0].toLowerCase();
            } else {
                row.email1 = ""; // Clear garbage if no valid email is found
            }
        }
        
        // Clean email2 just in case
        if (row.email2) {
            const str = String(row.email2);
            const match = str.match(emailRegex);
            if (match) {
                row.email2 = match[0].toLowerCase();
            } else {
                row.email2 = ""; 
            }
        }
    });

    // Enforce exact order including agid first
    const ws = xlsx.utils.json_to_sheet(data, {
        header: ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"]
    });
    xlsx.utils.book_append_sheet(newWb, ws, sheetName);
});

xlsx.writeFile(newWb, masterPath);
console.log("Email columns successfully cleaned across all sheets. Search blobs removed.");
