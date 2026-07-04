const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);

// Define mapping for long ranks to short ranks based on app rules
const rankMap = [
    { regex: /Director General and Inspector General of Police|DG\s*&\s*IGP/gi, replace: "DG & IGP" },
    { regex: /Director General of Police/gi, replace: "DGP" },
    { regex: /Additional Director General of Police|Addl\.? Director General of Police/gi, replace: "ADGP" },
    { regex: /Inspector General of Police/gi, replace: "IGP" },
    { regex: /Deputy Inspector General of Police|Dy\.? Inspector General of Police/gi, replace: "DIG" },
    { regex: /Superintendent of Police/gi, replace: "SP" },
    { regex: /Deputy Commissioner of Police/gi, replace: "DCP" },
    { regex: /Additional Superintendent of Police|Addl\.?\s*SP/gi, replace: "Addl.SP" },
    { regex: /Deputy Superintendent of Police|Dy\.?\s*SP/gi, replace: "DySP" },
    { regex: /Assistant Superintendent of Police|Assistant\s*SP/gi, replace: "ASP" },
    { regex: /Assistant Commissioner of Police/gi, replace: "ACP" },
    { regex: /Assistant Commandant/gi, replace: "ASST.CMDT" },
    { regex: /Deputy Commandant/gi, replace: "DEPT.CMDT" },
    { regex: /\bCommandant\b/gi, replace: "CMDT" },
    { regex: /Police Inspector/gi, replace: "PI" },
    { regex: /Police Sub[- ]Inspector|Sub[- ]Inspector of Police/gi, replace: "PSI" },
    { regex: /Assistant Sub[- ]Inspector/gi, replace: "ASI" },
    { regex: /\bHead Constable\b/gi, replace: "HC" },
    { regex: /Police Constable/gi, replace: "PC" }
];

const newWb = xlsx.utils.book_new();

wbSource.SheetNames.forEach(sheetName => {
    const data = xlsx.utils.sheet_to_json(wbSource.Sheets[sheetName]);
    
    data.forEach(row => {
        if (row.Name) {
            let newName = String(row.Name);
            
            // Apply mappings to the name string
            for (let mapping of rankMap) {
                // To avoid breaking things like "Addl.SP" if already processed, etc.
                // We do a global replace
                newName = newName.replace(mapping.regex, mapping.replace);
            }
            
            // Clean up any double spaces that might result
            newName = newName.replace(/\s{2,}/g, ' ').trim();
            row.Name = newName;
        }
    });

    // Create the sheet while forcing exact original headers
    const ws = xlsx.utils.json_to_sheet(data, {
        header: ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"]
    });
    xlsx.utils.book_append_sheet(newWb, ws, sheetName);
});

xlsx.writeFile(newWb, masterPath);
console.log("Rank shortening successfully applied to the 'Name' column across all sheets.");
