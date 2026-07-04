const fs = require('fs');
const path = require('path');

const realPath = path.join(__dirname, '..', 'officers_real_db.json');
const readyPath = path.join(__dirname, '..', 'officers_merge_ready.json');

const real = JSON.parse(fs.readFileSync(realPath, 'utf8'));
const ready = JSON.parse(fs.readFileSync(readyPath, 'utf8'));

console.log(`Real db entries: ${real.length}`);
console.log(`Ready db entries: ${ready.length}`);

// Count PI in real
const realPI = real.filter(o => o.rank === 'PI');
console.log(`Real entries with rank PI: ${realPI.length}`);

// Count PI in ready
const readyPI = ready.filter(o => o.rank === 'PI');
console.log(`Ready entries with rank PI: ${readyPI.length}`);

// Find entries where real rank is 'PI' but ready rank is different, or vice versa
const diff = [];
real.forEach(r => {
    const matching = ready.find(o => o.agid === r.agid);
    if (matching) {
        if (r.rank !== matching.rank) {
            diff.push({
                agid: r.agid,
                name: r.name,
                unit: r.unit,
                realRank: r.rank,
                readyRank: matching.rank
            });
        }
    } else {
        // Not found in ready
        diff.push({
            agid: r.agid,
            name: r.name,
            unit: r.unit,
            realRank: r.rank,
            readyRank: 'NOT_FOUND'
        });
    }
});

console.log(`Total rank differences: ${diff.length}`);
console.log('Sample differences (first 20):');
console.log(diff.slice(0, 20));

// Count how many of the differences have rank PI in real
const piDiff = diff.filter(d => d.realRank === 'PI');
console.log(`Differences where real rank is PI: ${piDiff.length}`);
console.log('Sample PI differences:');
console.log(piDiff.slice(0, 20));
