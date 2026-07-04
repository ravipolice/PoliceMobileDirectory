const fs = require('fs');
const path = require('path');

const readyPath = path.join(__dirname, '..', 'officers_merge_ready.json');
const ready = JSON.parse(fs.readFileSync(readyPath, 'utf8'));

const retired = ready.filter(o => /retd/i.test(o.rank) || /retd/i.test(o.name));
retired.slice(0, 15).forEach(o => {
    console.log(o.name);
});
