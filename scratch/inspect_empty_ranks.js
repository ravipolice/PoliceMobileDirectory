const fs = require('fs');
const path = require('path');

const readyPath = path.join(__dirname, '..', 'officers_merge_ready.json');
const ready = JSON.parse(fs.readFileSync(readyPath, 'utf8'));

const empty = ready.filter(o => o.rank === '');
console.log(`Total entries with empty rank: ${empty.length}`);
console.log('Sample empty rank entries (first 50):');
console.log(empty.slice(0, 50).map(o => ({
    agid: o.agid,
    name: o.name,
    unit: o.unit,
    district: o.district,
    subDivision: o.subDivision
})));
