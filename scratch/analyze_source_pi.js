const xlsx = require('xlsx');
const path = require('path');

const v3Path = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(v3Path);
const rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);

console.log(`Total rows in Excel MASTER_MERGED_FINAL: ${rows.length}`);

const ranks = rows.map(r => r.Rank || '');
const rankCounts = {};
ranks.forEach(r => {
    rankCounts[r] = (rankCounts[r] || 0) + 1;
});
console.log('Excel Rank Counts:');
console.log(rankCounts);

// Let's print out some rows where Rank is PI in Excel but we suspect they are not PIs.
// e.g. names with control room, retired, etc.
const suspectPIs = rows.filter(r => r.Rank === 'PI' && (
    /control|retired|retd|bmtc|kpcl|hesc|besc|gesc|deputation/i.test(r.Name) ||
    /control|room|admin|deputation/i.test(r.UNIT) ||
    /control/i.test(r.Section)
));

console.log(`\nSuspect PIs in Excel: ${suspectPIs.length}`);
console.log('Sample suspect PIs (first 10):');
console.log(suspectPIs.slice(0, 10).map(r => ({
    agid: r.agid,
    Name: r.Name,
    Rank: r.Rank,
    UNIT: r.UNIT,
    Section: r.Section
})));
