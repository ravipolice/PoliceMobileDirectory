const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);

const newWb = xlsx.utils.book_new();

wbSource.SheetNames.forEach(sheetName => {
    const data = xlsx.utils.sheet_to_json(wbSource.Sheets[sheetName]);
    
    data.forEach(row => {
        if (row.UNIT === "Others" || !row.UNIT) {
            const searchText = (row.Section + " " + row.Name + " " + (row.station || "")).toLowerCase();
            
            if (searchText.includes("intelligence") || searchText.includes(" int ")) {
                row.UNIT = "Intelligence";
            } else if (searchText.includes("admin") || searchText.includes("dpo") || searchText.includes("office") || searchText.includes("headquarter") || searchText.includes(" hq ")) {
                row.UNIT = "Admin";
            } else if (searchText.includes("traffic")) {
                row.UNIT = "Traffic";
            } else if (searchText.includes("cid") || searchText.includes("ccb")) {
                row.UNIT = "CID"; // Or CCB if CCB is a valid unit
            } else if (searchText.includes("dcre")) {
                row.UNIT = "DCRE";
            } else if (searchText.includes("isd")) {
                row.UNIT = "ISD";
            } else if (searchText.includes("ksrp")) {
                row.UNIT = "KSRP";
            } else {
                // Default regular civil police to L&O
                row.UNIT = "L&O";
            }
        }
    });

    const ws = xlsx.utils.json_to_sheet(data, {
        header: ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"]
    });
    xlsx.utils.book_append_sheet(newWb, ws, sheetName);
});

xlsx.writeFile(newWb, masterPath);
console.log("UNIT 'Others' intelligently reclassified to L&O, Admin, Intelligence, etc.");
