const fs = require('fs');
const path = require('path');

const realPath = path.join(__dirname, '..', 'officers_real_db.json');
const real = JSON.parse(fs.readFileSync(realPath, 'utf8'));

console.log(`Total real entries: ${real.length}`);

// Count how many have docId !== agid
let mismatchCount = 0;
const mismatches = [];

real.forEach(r => {
    if (r.docId !== r.agid) {
        mismatchCount++;
        mismatches.push({ docId: r.docId, agid: r.agid, name: r.name });
    }
});

console.log(`Mismatched docId vs agid count: ${mismatchCount}`);
console.log('Sample mismatches:');
console.log(mismatches.slice(0, 20));
