const fs = require('fs');
const path = require('path');

const readyPath = path.join(__dirname, '..', 'officers_merge_ready.json');
const ready = JSON.parse(fs.readFileSync(readyPath, 'utf8'));

console.log('Sample Active Officers Names:');
console.log(ready.filter(o => !/retd/i.test(o.rank) && !/retd/i.test(o.name)).slice(0, 15).map(o => o.name));

console.log('\nSample Retired Officers Names:');
console.log(ready.filter(o => /retd/i.test(o.rank) || /retd/i.test(o.name)).slice(0, 15).map(o => o.name));
