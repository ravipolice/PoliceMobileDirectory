const fs = require('fs');
const path = require('path');

const csvPath = path.join(__dirname, '..', 'KSP_Contacts_Master_Clean.csv');
const lines = fs.readFileSync(csvPath, 'utf8').split('\n');

const jsonPath = path.join(__dirname, '..', 'officers_real_db.json');
const realDb = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));

const ballariAaos = realDb.filter(row => {
    const unit = String(row.unit || "");
    const name = String(row.name || "");
    return unit.includes("Ballari") && (row.rank === "AAO" || name.includes("AAO"));
});

console.log("Matching JSON AAOs with original CSV:");
ballariAaos.forEach(jRow => {
    // Search in CSV lines for a phone or similar, but since phone is empty, let's see how they correspond.
    // Let's print out lines in CSV containing AAO and under Ballari Range.
    console.log(`JSON Row agid: ${jRow.agid}, name: ${jRow.name}, landline: ${jRow.landline}`);
});

console.log("\nCSV AAO rows in Ballari Range:");
lines.forEach((line, idx) => {
    if (line.includes("Ballari Range") && line.includes("AAO")) {
        console.log(`Line ${idx + 1}: ${line}`);
    }
});
