const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);

const fixMap = [
    { regex: /Director\s+General\s*(?:&|and)\s*IGP/gi, replace: "DG & IGP" },
    { regex: /DGP\s*(?:&|and)\s*IGP/gi, replace: "DG & IGP" },
    { regex: /DG\s*and\s*IGP/gi, replace: "DG & IGP" },
    { regex: /Director\s+General\s+of\s+Police\s*(?:&|and)\s*Inspector\s+General\s+of\s+Police/gi, replace: "DG & IGP" }
];

const newWb = xlsx.utils.book_new();

wbSource.SheetNames.forEach(sheetName => {
    const data = xlsx.utils.sheet_to_json(wbSource.Sheets[sheetName]);
    
    data.forEach(row => {
        // Fix Name column
        if (row.Name) {
            let newName = String(row.Name);
            for (let fix of fixMap) newName = newName.replace(fix.regex, fix.replace);
            row.Name = newName;
        }
        
        // Fix Rank column
        if (row.Rank) {
            let newRank = String(row.Rank);
            for (let fix of fixMap) newRank = newRank.replace(fix.regex, fix.replace);
            row.Rank = newRank;
        }
    });

    const ws = xlsx.utils.json_to_sheet(data, {
        header: ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"]
    });
    xlsx.utils.book_append_sheet(newWb, ws, sheetName);
});

xlsx.writeFile(newWb, masterPath);
console.log("Director General & IGP successfully shortened to DG & IGP.");
