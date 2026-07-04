const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);

// The exact official hierarchy from highest to lowest
const comprehensiveRankOrder = [
    "RETD. DG & IGP", "RETD. ADGP", "RETD. IGP",
    "DG & IGP", "Director General of Police", "DG", "DGP", 
    "Additional Director General of Police", "ADGP", 
    "Inspector General of Police", "IGP", 
    "Deputy Inspector General of Police", "DIG", "DIGP",
    "Commandant", "CMDT", "DCP", "Superintendent of Police", "SP", 
    "DEPT.CMDT", "ADDL_SP", "Addl. SP", "Addl SP", 
    "ASST.CMDT", "DSP", "DySP", "ACP",
    "CPI", "WPI", "PIW", "S.RPI", "RPI", "PI",
    "WPSI", "PSIW", "S.RSI", "RSI", "PSI",
    "S.ARSI", "ASIW", "ARSI", "WASI", "ASI",
    "HCW", "WHC", "S.RHC", "AHC", "CHC", "HC",
    "PCW", "WPC", "S.RPC", "APC", "CPC", "PC",
    "AO", "DD", "AD", "AAO", "CIO", "SIA", "IO", "AIO", "IA",
    "FDA", "SDA", "SS", "STENO", "TYPIST", "PA", "FOLLOWER"
];

function getRankScore(rankStr) {
    if (!rankStr) return 9999;
    const rankUpper = String(rankStr).toUpperCase();
    
    for (let i = 0; i < comprehensiveRankOrder.length; i++) {
        // Strict boundary check or exact inclusion to prevent "PI" matching "CPI" incorrectly if order was wrong
        // But our array is ordered specifically to catch CPI before PI.
        if (rankUpper.includes(comprehensiveRankOrder[i].toUpperCase())) {
            return i;
        }
    }
    return 9999; // Unknown ranks go to the bottom
}

// Function to sort an array of row objects
function sortData(data) {
    return data.sort((a, b) => {
        const scoreA = getRankScore(a.Rank);
        const scoreB = getRankScore(b.Rank);
        return scoreA - scoreB;
    });
}

const newWb = xlsx.utils.book_new();

// 1. Process Master Sheet first to ensure it is the first tab
const masterData = xlsx.utils.sheet_to_json(wbSource.Sheets['MASTER_MERGED_FINAL']);
const sortedMaster = sortData(masterData);
const wsMaster = xlsx.utils.json_to_sheet(sortedMaster);
xlsx.utils.book_append_sheet(newWb, wsMaster, "MASTER_MERGED_FINAL");

// 2. Process all other sheets
wbSource.SheetNames.forEach(sheetName => {
    if (sheetName !== "MASTER_MERGED_FINAL") {
        const sheetData = xlsx.utils.sheet_to_json(wbSource.Sheets[sheetName]);
        const sortedSheetData = sortData(sheetData);
        const ws = xlsx.utils.json_to_sheet(sortedSheetData);
        xlsx.utils.book_append_sheet(newWb, ws, sheetName);
    }
});

xlsx.writeFile(newWb, masterPath);
console.log(`Comprehensive sort complete across all ${wbSource.SheetNames.length} sheets.`);
