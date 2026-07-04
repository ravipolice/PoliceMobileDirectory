const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);
const data = xlsx.utils.sheet_to_json(wbSource.Sheets['MASTER_MERGED_FINAL']);

console.log(`Original count: ${data.length}`);

const uniqueMap = new Map();

// Helper to clean mobile
function cleanMobile(m) {
    if (!m) return "";
    const s = String(m).replace(/[^0-9]/g, "");
    return s.length >= 10 ? s.slice(-10) : "";
}

// Helper to clean encoding
function cleanText(t) {
    return String(t || "").replace(/[â\x80-\x9F]/g, "").trim();
}

data.forEach(row => {
    const agid = cleanText(row.agid);
    const mobile = cleanMobile(row["Mobile 1"] || row["Mobile 2"]);
    const name = cleanText(row.Name).toLowerCase();
    
    // Generate a unique key: Priority AGID, then Mobile, then Name
    const key = agid || (mobile ? `mob_${mobile}` : `name_${name}`);
    
    if (!uniqueMap.has(key)) {
        uniqueMap.set(key, row);
    } else {
        // Merge data if existing record is less complete
        const existing = uniqueMap.get(key);
        if (!existing["Office 1"] && row["Office 1"]) existing["Office 1"] = row["Office 1"];
        if (!existing.Email && row.Email) existing.Email = row.Email;
        if (!existing.Station && row.Station) existing.Station = row.Station;
    }
});

let finalData = Array.from(uniqueMap.values());
console.log(`Deduplicated count: ${finalData.length}`);

// RE-SORT
const rankOrder = [
    "DG & IGP", "Director General of Police", "Addl. Director General of Police", 
    "ADGP", "Inspector General of Police", "IGP", "DIGP", "Deputy Inspector General of Police",
    "Superintendent of Police", "SP", "DCP", "Addl. SP", "DySP"
];

finalData.sort((a, b) => {
    const rankA = String(a.Rank || "");
    const rankB = String(b.Rank || "");
    let indexA = rankOrder.findIndex(r => rankA.includes(r));
    let indexB = rankOrder.findIndex(r => rankB.includes(r));
    if (indexA === -1) indexA = 999;
    if (indexB === -1) indexB = 999;
    return indexA - indexB;
});

// Save Cleaned Master
const newWb = xlsx.utils.book_new();
const wsMaster = xlsx.utils.json_to_sheet(finalData);
xlsx.utils.book_append_sheet(newWb, wsMaster, "MASTER_MERGED_FINAL");

xlsx.writeFile(newWb, masterPath);
console.log("Strict Deduplication and Re-Sort complete.");
