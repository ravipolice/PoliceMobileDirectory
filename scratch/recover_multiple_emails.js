const xlsx = require('xlsx');
const path = require('path');
const fs = require('fs');

const v3Path = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const v2Path = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V2.xlsx');

if (!fs.existsSync(v2Path)) {
    console.error("V2 not found. Cannot restore emails.");
    process.exit(1);
}

// Global regex to extract ALL valid emails from a string
const emailRegex = /[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/g;

// 1. Read V2 to build a map of pure, extracted emails for each AGID
const wbV2 = xlsx.readFile(v2Path);
const v2Master = xlsx.utils.sheet_to_json(wbV2.Sheets['MASTER_MERGED_FINAL'] || wbV2.Sheets[wbV2.SheetNames[0]]);

const emailMap = {};
v2Master.forEach(row => {
    let agid = row.agid || row.AGID;
    if (agid) {
        let allEmails = [];
        
        // Extract all from V2 email1
        if (row.email1) {
            let matches = String(row.email1).match(emailRegex);
            if (matches) allEmails.push(...matches.map(e => e.toLowerCase()));
        }
        
        // Extract all from V2 email2
        if (row.email2) {
            let matches = String(row.email2).match(emailRegex);
            if (matches) allEmails.push(...matches.map(e => e.toLowerCase()));
        }
        
        // Deduplicate the found emails
        allEmails = [...new Set(allEmails)];
        
        emailMap[agid] = {
            email1: allEmails.length > 0 ? allEmails[0] : "",
            email2: allEmails.length > 1 ? allEmails[1] : ""
        };
    }
});

// 2. Read V3 and update it with the recovered emails
const wbV3 = xlsx.readFile(v3Path);
const newWb = xlsx.utils.book_new();

wbV3.SheetNames.forEach(sheetName => {
    const data = xlsx.utils.sheet_to_json(wbV3.Sheets[sheetName]);
    
    data.forEach(row => {
        let agid = row.agid || row.AGID;
        if (agid && emailMap[agid]) {
            row.email1 = emailMap[agid].email1;
            row.email2 = emailMap[agid].email2;
        }
    });

    const ws = xlsx.utils.json_to_sheet(data, {
        header: ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"]
    });
    xlsx.utils.book_append_sheet(newWb, ws, sheetName);
});

xlsx.writeFile(newWb, v3Path);
console.log("Emails securely restored from V2 and re-extracted to capture multiple emails per contact.");
