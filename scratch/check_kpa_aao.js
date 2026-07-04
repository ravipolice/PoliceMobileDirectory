const fs = require('fs');
const path = require('path');

const jsonPath = path.join(__dirname, '..', 'officers_merge_ready.json');
if (!fs.existsSync(jsonPath)) {
    console.error("JSON file does not exist");
    process.exit(1);
}

const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
const kpaO = data.filter(o => o.email === 'kpamyc@ksp.gov.in' || o.landline.includes('2418636') || o.name.includes('KPA') || o.name.includes('Academy'));

console.log(`Found ${kpaO.length} matching records:`);
kpaO.forEach(o => {
    console.log(`ID: ${o.agid} | Name: "${o.name}" | Rank: "${o.rank}" | Unit: "${o.unit}" | District: "${o.district}" | SubDivision: "${o.subDivision}"`);
});
