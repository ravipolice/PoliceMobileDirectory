const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);

const nameFixes = [
    { regex: /Deputy\s+SP/gi, replace: "DySP" },
    { regex: /Assistant\s+SP/gi, replace: "ASP" },
    { regex: /Additional\s+SP/gi, replace: "Addl.SP" },
    // Just in case any full forms were missed
    { regex: /Deputy\s+Superintendent\s+of\s+Police/gi, replace: "DySP" },
    { regex: /Assistant\s+Superintendent\s+of\s+Police/gi, replace: "ASP" },
    { regex: /Additional\s+Superintendent\s+of\s+Police/gi, replace: "Addl.SP" },
    // Fix Addl SP without dot
    { regex: /\bAddl\s+SP\b/gi, replace: "Addl.SP" }
];

const newWb = xlsx.utils.book_new();

wbSource.SheetNames.forEach(sheetName => {
    const data = xlsx.utils.sheet_to_json(wbSource.Sheets[sheetName]);
    
    data.forEach(row => {
        if (row.Name) {
            let newName = String(row.Name);
            
            for (let fix of nameFixes) {
                newName = newName.replace(fix.regex, fix.replace);
            }
            
            row.Name = newName;
        }
    });

    // Enforce exact order including agid first
    const ws = xlsx.utils.json_to_sheet(data, {
        header: ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"]
    });
    xlsx.utils.book_append_sheet(newWb, ws, sheetName);
});

xlsx.writeFile(newWb, masterPath);
console.log("Missed SP ranks in the Name column successfully shortened.");
