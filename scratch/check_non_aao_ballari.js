const fs = require('fs');
const path = require('path');

const jsonPath = path.join(__dirname, '..', 'officers_real_db.json');
const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));

// Find contacts under Ballari Range - Ballari unit
const ballariContacts = data.filter(row => {
    return row.unit === "Ballari Range – Ballari" && !row.name.includes("AAO");
}).slice(0, 10);

console.log("Sample non-AAO Ballari Range contacts:");
ballariContacts.forEach(row => {
    console.log({
        name: row.name,
        rank: row.rank,
        district: row.district,
        office: row.office
    });
});
