const fs = require('fs');
const path = require('path');

const readyPath = path.join(__dirname, '..', 'officers_merge_ready.json');
const ready = JSON.parse(fs.readFileSync(readyPath, 'utf8'));

console.log(`Total entries in ready: ${ready.length}`);

const rankCounts = {};
ready.forEach(o => {
    const r = o.rank || '';
    rankCounts[r] = (rankCounts[r] || 0) + 1;
});

console.log('Ranks in officers_merge_ready.json:');
console.log(rankCounts);

// Let's check some retired officers in ready
const retiredInReady = ready.filter(o => /retd/i.test(o.rank) || /retd/i.test(o.name));
console.log(`\nRetired officers in ready: ${retiredInReady.length}`);
console.log('Sample retired officers in ready:');
console.log(retiredInReady.slice(0, 10).map(o => ({ agid: o.agid, name: o.name, rank: o.rank })));

// Let's check control rooms in ready
const controlRoomsInReady = ready.filter(o => /control/i.test(o.name));
console.log(`\nControl rooms in ready: ${controlRoomsInReady.length}`);
console.log('Sample control rooms in ready:');
console.log(controlRoomsInReady.slice(0, 10).map(o => ({ agid: o.agid, name: o.name, rank: o.rank })));
