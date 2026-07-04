const fs = require('fs');
const path = require('path');

const realPath = path.join(__dirname, '..', 'officers_real_db.json');
const real = JSON.parse(fs.readFileSync(realPath, 'utf8'));

const suspects = real.filter(o => o.rank === 'PI' && (
    /retd|retired/i.test(o.name) ||
    /control\s+room/i.test(o.name)
));

console.log(`Suspect PIs in officers_real_db.json: ${suspects.length}`);
console.log('Sample suspects (first 20):');
console.log(suspects.slice(0, 20).map(s => ({ agid: s.agid, name: s.name, rank: s.rank, unit: s.unit })));
