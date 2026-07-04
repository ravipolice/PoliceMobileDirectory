const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);

const newWb = xlsx.utils.book_new();

const desiredHeaders = [
    "agid", "UNIT", "Range", "District", "Section", "Name", 
    "Rank", "station", "office1", "office 2", "mobile 1", 
    "mobile 2", "email1", "email2"
];

wbSource.SheetNames.forEach(sheetName => {
    const data = xlsx.utils.sheet_to_json(wbSource.Sheets[sheetName]);
    
    // Create new array with explicitly ordered keys
    const reorderedData = data.map(row => {
        const newRow = {};
        
        // Always set agid first
        newRow["agid"] = row["agid"] || row["AGID"] || "";
        
        // Copy the rest in the defined order
        for (let i = 1; i < desiredHeaders.length; i++) {
            const key = desiredHeaders[i];
            newRow[key] = row[key] !== undefined ? row[key] : "";
        }
        
        return newRow;
    });

    // Create the sheet using header option to enforce order
    const ws = xlsx.utils.json_to_sheet(reorderedData, { header: desiredHeaders });
    xlsx.utils.book_append_sheet(newWb, ws, sheetName);
});

xlsx.writeFile(newWb, masterPath);
console.log("AGID successfully moved to the first column across all sheets.");
