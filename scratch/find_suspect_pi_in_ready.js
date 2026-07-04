const fs = require('fs');
const path = require('path');

const readyPath = path.join(__dirname, '..', 'officers_merge_ready.json');
const ready = JSON.parse(fs.readFileSync(readyPath, 'utf8'));

const suspects = ready.filter(o => o.rank === 'PI' && (
    /retd|retired|control|room|admin|deputation/i.test(o.name) ||
    /retd|retired|control|room|admin|deputation/i.test(o.unit) ||
    /retd|retired|control|room|admin|deputation/i.test(o.subDivision)
));

console.log(`Suspect PIs in officers_merge_ready.json: ${suspects.length}`);
if (suspects.length > 0) {
    console.log('Suspects list:');
    console.log(suspects.map(s => ({ agid: s.agid, name: s.name, rank: s.rank, unit: s.unit })));
} else {
    console.log('✅ No suspect PIs found in officers_merge_ready.json!');
}
