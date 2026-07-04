const xlsx = require('xlsx');
const path = require('path');
const fs = require('fs');

const v3Path = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const v2Path = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V2.xlsx');

if (!fs.existsSync(v2Path)) {
    console.error("V2 not found. Cannot restore.");
    process.exit(1);
}

const wbV2 = xlsx.readFile(v2Path);
const v2Master = xlsx.utils.sheet_to_json(wbV2.Sheets['MASTER_MERGED_FINAL'] || wbV2.Sheets[wbV2.SheetNames[0]]);

const sectionMap = {};
v2Master.forEach(row => {
    let agid = row.agid || row.AGID;
    if (agid) {
        sectionMap[agid] = row.Section || row.SECTION || "";
    }
});

const wbV3 = xlsx.readFile(v3Path);
const newWb = xlsx.utils.book_new();

wbV3.SheetNames.forEach(sheetName => {
    const data = xlsx.utils.sheet_to_json(wbV3.Sheets[sheetName]);
    
    data.forEach(row => {
        let agid = row.agid || row.AGID;
        if (agid && sectionMap[agid]) {
            // Restore original section (the minor head)
            row.Section = sectionMap[agid];
        }
    });

    const ws = xlsx.utils.json_to_sheet(data, {
        header: ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"]
    });
    xlsx.utils.book_append_sheet(newWb, ws, sheetName);
});

xlsx.writeFile(newWb, v3Path);
console.log("Original minor head 'Section' data successfully restored from V2 backup.");
