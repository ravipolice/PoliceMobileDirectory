const fs = require('fs');
const path = require('path');

const jsonPath = path.join(__dirname, '..', 'officers_real_db.json');
const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));

const ballariAaos = data.filter(row => {
    const unit = String(row.unit || "");
    const name = String(row.name || "");
    return unit.includes("Ballari") && (row.rank === "AAO" || name.includes("AAO"));
});

console.log("Ballari AAO records details:");
console.log(JSON.stringify(ballariAaos, null, 2));
