const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);

let allData = [];
wb.SheetNames.forEach(name => {
    const sheet = wb.Sheets[name];
    const data = xlsx.utils.sheet_to_json(sheet);
    allData = allData.concat(data);
});

const ranks = allData.map(r => String(r.Rank || '').trim());
const rankCounts = {};
ranks.forEach(r => {
    rankCounts[r] = (rankCounts[r] || 0) + 1;
});

const sortedRanks = Object.entries(rankCounts).sort((a, b) => b[1] - a[1]);

console.log('Top 50 Ranks:');
console.table(sortedRanks.slice(0, 50));
