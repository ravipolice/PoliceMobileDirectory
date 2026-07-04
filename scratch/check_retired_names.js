const fs = require('fs');
const path = require('path');

const readyPath = path.join(__dirname, '..', 'officers_merge_ready.json');
const ready = JSON.parse(fs.readFileSync(readyPath, 'utf8'));

const retired = ready.filter(o => /retd/i.test(o.unit) || /retd/i.test(o.rank) || /retd/i.test(o.name));
console.log(`Total retired officers: ${retired.length}`);
retired.forEach(o => {
    console.log(`agid: ${o.agid} | Name: "${o.name}" | Rank: "${o.rank}"`);
});
