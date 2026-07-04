const xlsx = require('xlsx');
const fs = require('fs');
const path = require('path');

const csvPath = path.join(__dirname, '..', 'KSP_Officers_App.csv');
const wbCsv = xlsx.readFile(csvPath);
const rawData = xlsx.utils.sheet_to_json(wbCsv.Sheets[wbCsv.SheetNames[0]]);

const fixedData = [];

// Helper to identify and route data to correct columns
function processRow(r) {
    // 1. Combine all values from the row to find data
    const allValues = Object.values(r).map(v => String(v).trim());
    
    let mobile = "";
    let landline = "";
    let email = "";
    
    allValues.forEach(v => {
        if (v.includes('@')) email = v;
        else if (v.match(/^[789]\d{9}$/)) mobile = v; // 10 digit mobile starting with 7,8,9
        else if (v.match(/^\d{10}$/)) mobile = v;
        else if (v.match(/^0\d{2,4}-\d+$/)) landline = v; // landline with STD
        else if (v.match(/^\d{6,8}$/)) landline = v; // landline without STD
    });

    // Clean characters like â
    const clean = (text) => String(text || "").replace(/[â\x80-\x9F]/g, "").trim();

    return {
        Section: clean(r.unit),
        Unit: clean(r.unit),
        District: clean(r.district),
        Name: clean(r.name),
        Rank: clean(r.rank),
        Station: clean(r.station),
        "Office 1": landline || clean(r.landline),
        "Office 2": "",
        "Mobile 1": mobile || clean(r.mobile),
        "Mobile 2": "",
        Email: clean(email || r.email),
        agid: r.agid
    };
}

rawData.forEach(row => {
    fixedData.push(processRow(row));
});

// 2. TOP SORT LOGIC
const rankOrder = [
    "DG & IGP", "Director General of Police", "Addl. Director General of Police", 
    "ADGP", "Inspector General of Police", "IGP", "DIGP", "Deputy Inspector General of Police",
    "Superintendent of Police", "SP", "DCP", "Addl. SP", "DySP"
];

fixedData.sort((a, b) => {
    const rankA = String(a.Rank);
    const rankB = String(b.Rank);
    
    let indexA = rankOrder.findIndex(r => rankA.includes(r));
    let indexB = rankOrder.findIndex(r => rankB.includes(r));
    
    if (indexA === -1) indexA = 999;
    if (indexB === -1) indexB = 999;
    
    return indexA - indexB;
});

// 3. Save to Excel
const newWb = xlsx.utils.book_new();
const wsMaster = xlsx.utils.json_to_sheet(fixedData);
xlsx.utils.book_append_sheet(newWb, wsMaster, "MASTER_MERGED_FINAL");

const finalPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
xlsx.writeFile(newWb, finalPath);

console.log(`Rebuild and Top-Sort complete. Fixed ${fixedData.length} rows.`);
