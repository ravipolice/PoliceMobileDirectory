const fs = require('fs');
const path = require('path');

const jsonPath = path.join(__dirname, '..', 'officers_merge_ready.json');
if (!fs.existsSync(jsonPath)) {
    console.error("JSON file does not exist");
    process.exit(1);
}

const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
const badPIs = data.filter(o => o.rank === 'PI' && (o.name.toLowerCase().includes('typist') || o.searchBlob.includes('typist')));

console.log(`Found ${badPIs.length} suspected TYPISTs mapped to PI:`);
badPIs.forEach(o => {
    console.log(`ID: ${o.agid} | Name: "${o.name}" | Rank: "${o.rank}" | Blob: "${o.searchBlob}"`);
});
