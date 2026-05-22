const xlsx = require('xlsx');
const fs = require('fs');
const path = require('path');
const ExcelJS = require('exceljs');

const v3Path = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');

const KARNATAKA_DISTRICTS = [
    "Bagalkote", "Ballari", "Belagavi", "Bengaluru City", "Bengaluru Rural", 
    "Bidar", "Chamarajanagar", "Chikkaballapura", "Chikkamagaluru", "Chitradurga", 
    "Dakshina Kannada", "Davanagere", "Dharwad", "Gadag", "Hassan", "Haveri", 
    "Kalaburagi", "Kodagu", "Kolar", "Koppal", "Mandya", "Mysuru City", "Mysuru District",
    "Raichur", "Ramanagara", "Shivamogga", "Tumakuru", "Udupi", "Uttara Kannada", 
    "Vijayanagara", "Vijayapura", "Yadgir", "Hubballi-Dharwad City", "Mangaluru City", "Belagavi City", "Kalaburagi City"
];

async function refineDistricts() {
    console.log("Starting Smart District Refinement...");
    const wb = xlsx.readFile(v3Path);
    const rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);

    let fixedCount = 0;

    rows.forEach(r => {
        let name = String(r.Name || "");
        let unit = String(r.UNIT || "");
        let district = String(r.District || "");

        // If district is a Range name or generic, try to find a specific district in the Name
        if (district.includes("Range") || !district) {
            for (const d of KARNATAKA_DISTRICTS) {
                if (name.includes(d) || unit.includes(d)) {
                    r.District = d;
                    fixedCount++;
                    break;
                }
            }
        }
        
        // Also cleanup cases where Unit == District
        if (r.UNIT === r.District && r.UNIT.includes("Range")) {
             // If it's a range, see if we can find a better district
             // (Already handled by loop above, but this confirms we tried)
        }
    });

    console.log(`Refined ${fixedCount} districts.`);

    // Save back to Excel
    const workbook = new ExcelJS.Workbook();
    const headers = ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"];
    const ws = workbook.addWorksheet('MASTER_MERGED_FINAL');
    ws.columns = headers.map(h => ({ header: h, key: h, width: 20 }));
    rows.forEach(r => ws.addRow(r));
    ws.getRow(1).font = { bold: true };
    await workbook.xlsx.writeFile(v3Path);

    console.log("Excel file updated.");
}

refineDistricts().catch(console.error);
