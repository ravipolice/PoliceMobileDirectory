const xlsx = require('xlsx');
const fs = require('fs');
const path = require('path');

// 1. Load the App CSV (7394 rows)
const csvPath = path.join(__dirname, '..', 'KSP_Officers_App.csv');
const wbCsv = xlsx.readFile(csvPath);
const appData = xlsx.utils.sheet_to_json(wbCsv.Sheets[wbCsv.SheetNames[0]]);

// 2. Load the Website Data (from previous scrape chunks)
// I'll define the new contacts found for Bagalkote, Gadag, Vijayapura here
const websiteContacts = [
    // Bagalkote Recovered
    { district: "Bagalkote", unit: "L&O", name: "SP, Bagalakote", rank: "SP", office: "08354-235077", mobile: "9480803901", email: "spbgk@ksp.gov.in", station: "District Office" },
    { district: "Bagalkote", unit: "L&O", name: "Addl. SP-1, Bagalakote", rank: "Addl. SP-1", office: "235254", mobile: "9480803902", email: "addlspbgk@ksp.gov.in", station: "District Office" },
    { district: "Bagalkote", unit: "L&O", name: "DSP Bagalkote", rank: "DSP", office: "220423", mobile: "9480803920", email: "sdpobgk@ksp.gov.in", station: "Bagalkote Sub Division" },
    { district: "Bagalkote", unit: "L&O", name: "PI Bagalkot Town PS", rank: "PI", office: "220333", mobile: "9480803945", email: "bgktownbgk@ksp.gov.in", station: "Bagalkote Town PS" },
    { district: "Bagalkote", unit: "L&O", name: "CPI Hunagund Circle", rank: "CPI", office: "260333", mobile: "9480803933", email: "cpihinagundbgk@ksp.gov.in", station: "Hunagund Circle" },
    { district: "Bagalkote", unit: "L&O", name: "CPI Badami Circle", rank: "CPI", office: "220033", mobile: "9480803932", email: "cpibadamibgk@ksp.gov.in", station: "Badami Circle" },
    
    // Gadag Recovered
    { district: "Gadag", unit: "L&O", name: "SP, Gadag", rank: "SP", office: "08372-237300", mobile: "9480804401", email: "spgdg@ksp.gov.in", station: "District Office" },
    { district: "Gadag", unit: "L&O", name: "PI Gadag Town PS", rank: "PI", office: "235233", mobile: "9480804430", email: "towngdg@ksp.gov.in", station: "Gadag Town PS" },
    { district: "Gadag", unit: "L&O", name: "PI Gadag Rural PS", rank: "PI", office: "278703", mobile: "9480804431", email: "cpiruralgdg@ksp.gov.in", station: "Gadag Rural PS" },
    { district: "Gadag", unit: "L&O", name: "CPI Shirahatti Circle", rank: "CPI", office: "242434", mobile: "9480804433", email: "cpishirahattigdg@ksp.gov.in", station: "Shirahatti Circle" },
    { district: "Gadag", unit: "L&O", name: "PI Mundargi PS", rank: "PI", office: "262233", mobile: "9480804455", email: "mundaragigdg@ksp.gov.in", station: "Mundargi PS" },
    
    // Vijayapura Recovered
    { district: "Vijayapura", unit: "L&O", name: "SP, Vijayapura", rank: "SP", office: "08352-250152", mobile: "9480804201", email: "spbjp@ksp.gov.in", station: "District Office" },
    { district: "Vijayapura", unit: "L&O", name: "Addl.SP-1, Vijayapura", rank: "Addl.SP-1", office: "250040", mobile: "9480804202", email: "addlspbjp@ksp.gov.in", station: "District Office" },
    { district: "Vijayapura", unit: "L&O", name: "District Control Room", rank: "C/Room", office: "250751 / 250948", mobile: "9480804200", email: "dcbjp@ksp.gov.in", station: "Control Room" }
];

// 3. Create a consolidated Master list
const masterList = [];

// Helper to clean mobile
function cleanMobile(m) {
    if (!m) return "";
    const s = String(m);
    return s.replace(/[^0-9]/g, "").slice(-10);
}

// Map CSV rows to standard format
appData.forEach(r => {
    masterList.push({
        Section: r.unit || "",
        Unit: r.unit || "",
        District: r.district || "",
        Name: r.name || "",
        Rank: r.rank || "",
        Station: r.station || "",
        "Office 1": r.landline || "",
        "Office 2": "",
        "Mobile 1": r.mobile || "",
        "Mobile 2": "",
        Email: r.email || "",
        Source: "App CSV"
    });
});

// Add Website Data (if not already there)
websiteContacts.forEach(wc => {
    const mobile = cleanMobile(wc.mobile);
    const exists = masterList.some(r => cleanMobile(r["Mobile 1"]) === mobile && mobile !== "");
    if (!exists) {
        masterList.push({
            Section: "Ranges",
            Unit: wc.unit || "L&O",
            District: wc.district,
            Name: wc.name,
            Rank: wc.rank,
            Station: wc.station,
            "Office 1": wc.office,
            "Office 2": "",
            "Mobile 1": wc.mobile,
            "Mobile 2": "",
            Email: wc.email,
            Source: "Website Fetch"
        });
    }
});

// 4. Save to a new Excel File
const newWb = xlsx.utils.book_new();
const wsMaster = xlsx.utils.json_to_sheet(masterList);
xlsx.utils.book_append_sheet(newWb, wsMaster, "MASTER_MERGED_FINAL");

const finalPath = path.join(__dirname, '..', 'KSP_Contacts_Super_Master.xlsx');
xlsx.writeFile(newWb, finalPath);

console.log(`Successfully created Super Master with ${masterList.length} total contacts.`);
console.log(`Includes ${websiteContacts.length} recovered contacts for Bagalkote, Gadag, Vijayapura.`);
