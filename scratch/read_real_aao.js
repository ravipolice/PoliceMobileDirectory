const fs = require('fs');
const path = require('path');

const jsonPath = path.join(__dirname, '..', 'officers_real_db.json');
const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));

const aaos = data.filter(row => {
    const name = String(row.name || "");
    const rank = String(row.rank || "");
    return rank === "AAO" || name.includes("AAO");
});

console.log(`Found ${aaos.length} AAO records in officers_real_db.json:`);
aaos.forEach(row => {
    console.log({
        agid: row.agid || row.kgid,
        name: row.name,
        rank: row.rank,
        unit: row.unit,
        district: row.district,
        station: row.station
    });
});
