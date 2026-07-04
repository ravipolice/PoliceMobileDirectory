const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);

// Comprehensive map to catch all possible acronym variations
const rankMap = [
    { regex: /Additional\s+Director\s+General\s+of\s+Police|Addl\.?\s*Director\s+General\s+of\s+Police|Additional\s+DGP|Addl\.?\s*DGP/gi, replace: "ADGP" },
    { regex: /Director\s+General\s+and\s+Inspector\s+General\s+of\s+Police|DG\s*&\s*IGP/gi, replace: "DG & IGP" },
    { regex: /Director\s+General\s+of\s+Police/gi, replace: "DGP" },
    { regex: /Inspector\s+General\s+of\s+Police/gi, replace: "IGP" },
    { regex: /Deputy\s+Inspector\s+General\s+of\s+Police|Dy\.?\s*IGP|Deputy\s+IGP/gi, replace: "DIG" },
    { regex: /Additional\s+Superintendent\s+of\s+Police|Addl\.?\s*Superintendent\s+of\s+Police|Additional\s+SP|Addl\.?\s*SP/gi, replace: "Addl.SP" },
    { regex: /Deputy\s+Superintendent\s+of\s+Police|Dy\.?\s*Superintendent\s+of\s+Police|Deputy\s+SP|Dy\.?\s*SP/gi, replace: "DySP" },
    { regex: /Assistant\s+Superintendent\s+of\s+Police|Asst\.?\s*Superintendent\s+of\s+Police|Assistant\s+SP|Asst\.?\s*SP/gi, replace: "ASP" },
    { regex: /Superintendent\s+of\s+Police/gi, replace: "SP" },
    { regex: /Deputy\s+Commissioner\s+of\s+Police|Deputy\s+CP/gi, replace: "DCP" },
    { regex: /Assistant\s+Commissioner\s+of\s+Police|Asst\.?\s*Commissioner\s+of\s+Police|Assistant\s+CP|Asst\.?\s*CP/gi, replace: "ACP" },
    { regex: /Assistant\s+Commandant|Asst\.?\s*Cmdt/gi, replace: "ASST.CMDT" },
    { regex: /Deputy\s+Commandant|Dy\.?\s*Cmdt/gi, replace: "DEPT.CMDT" },
    { regex: /\bCommandant\b/gi, replace: "CMDT" },
    { regex: /Police\s+Inspector/gi, replace: "PI" },
    { regex: /Police\s+Sub[- ]Inspector|Sub[- ]Inspector\s+of\s+Police/gi, replace: "PSI" },
    { regex: /Assistant\s+Sub[- ]Inspector|Asst\.?\s*Sub[- ]Inspector/gi, replace: "ASI" },
    { regex: /\bHead\s+Constable\b/gi, replace: "HC" },
    { regex: /\bPolice\s+Constable\b/gi, replace: "PC" }
];

// Clean up redundant doubles like "ADGP ADGP"
const doubleFixes = [
    { regex: /\bADGP\s+ADGP\b/gi, replace: "ADGP" },
    { regex: /\bIGP\s+IGP\b/gi, replace: "IGP" },
    { regex: /\bDGP\s+DGP\b/gi, replace: "DGP" },
    { regex: /\bDIG\s+DIG\b/gi, replace: "DIG" },
    { regex: /\bSP\s+SP\b/gi, replace: "SP" },
    { regex: /\bDySP\s+DySP\b/gi, replace: "DySP" },
    { regex: /\bAddl\.SP\s+Addl\.SP\b/gi, replace: "Addl.SP" },
    { regex: /\bASP\s+ASP\b/gi, replace: "ASP" }
];

const newWb = xlsx.utils.book_new();

wbSource.SheetNames.forEach(sheetName => {
    const data = xlsx.utils.sheet_to_json(wbSource.Sheets[sheetName]);
    
    data.forEach(row => {
        // Clean Name Column
        if (row.Name) {
            let newName = String(row.Name);
            for (let mapping of rankMap) newName = newName.replace(mapping.regex, mapping.replace);
            for (let fix of doubleFixes) newName = newName.replace(fix.regex, fix.replace);
            row.Name = newName.replace(/\s{2,}/g, ' ').trim();
        }

        // Clean Rank Column
        if (row.Rank) {
            let originalRank = String(row.Rank);
            let isRetd = /^RETD\.\s*|^Retired\s*/i.test(originalRank);
            if (isRetd) originalRank = originalRank.replace(/^RETD\.\s*|^Retired\s*/i, '').trim();

            let newRank = originalRank;
            for (let mapping of rankMap) newRank = newRank.replace(mapping.regex, mapping.replace);
            for (let fix of doubleFixes) newRank = newRank.replace(fix.regex, fix.replace);

            if (isRetd) newRank = "RETD. " + newRank;
            row.Rank = newRank.replace(/\s{2,}/g, ' ').trim();
        }
    });

    const ws = xlsx.utils.json_to_sheet(data, {
        header: ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"]
    });
    xlsx.utils.book_append_sheet(newWb, ws, sheetName);
});

xlsx.writeFile(newWb, masterPath);
console.log("Deep rank abbreviation (including variations like Additional DGP) complete.");
