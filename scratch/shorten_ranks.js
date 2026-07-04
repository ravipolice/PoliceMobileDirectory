const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);

// Define mapping for long ranks to short ranks based on app rules
const rankMap = [
    { regex: /Director General of Police/i, replace: "DGP" },
    { regex: /Additional Director General of Police|Addl\. Director General of Police/i, replace: "ADGP" },
    { regex: /Inspector General of Police/i, replace: "IGP" },
    { regex: /Deputy Inspector General of Police|Dy\. Inspector General of Police/i, replace: "DIG" },
    { regex: /Superintendent of Police/i, replace: "SP" },
    { regex: /Deputy Commissioner of Police/i, replace: "DCP" },
    { regex: /Additional Superintendent of Police|Addl\. SP|Addl SP/i, replace: "ADDL_SP" },
    { regex: /Deputy Superintendent of Police|DySP|Dy\. SP/i, replace: "DSP" },
    { regex: /Assistant Commissioner of Police/i, replace: "ACP" },
    { regex: /Assistant Commandant/i, replace: "ASST.CMDT" },
    { regex: /Deputy Commandant/i, replace: "DEPT.CMDT" },
    { regex: /^Commandant$/i, replace: "CMDT" },
    { regex: /Police Inspector/i, replace: "PI" },
    { regex: /Police Sub[- ]Inspector|Sub[- ]Inspector of Police/i, replace: "PSI" },
    { regex: /Assistant Sub[- ]Inspector/i, replace: "ASI" },
    { regex: /^Head Constable$/i, replace: "HC" },
    { regex: /Police Constable/i, replace: "PC" }
];

const newWb = xlsx.utils.book_new();

wbSource.SheetNames.forEach(sheetName => {
    const data = xlsx.utils.sheet_to_json(wbSource.Sheets[sheetName]);
    
    data.forEach(row => {
        if (row.Rank) {
            let originalRank = String(row.Rank);
            
            // First fix RETD prefix if exists
            let isRetd = /RETD|Retired/i.test(originalRank);
            if (isRetd) {
                originalRank = originalRank.replace(/RETD\.?|Retired/i, '').trim();
            }

            let newRank = originalRank;
            
            // Apply mappings
            for (let mapping of rankMap) {
                if (mapping.regex.test(newRank)) {
                    newRank = newRank.replace(mapping.regex, mapping.replace).trim();
                    break; 
                }
            }
            
            // Special fix for "DG & IGP" if it became "DGP" when it shouldn't have
            if (originalRank.includes("DG & IGP") || originalRank.includes("Director General and Inspector General")) {
                newRank = "DG & IGP";
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
console.log("Rank shortening successfully applied to all sheets.");
