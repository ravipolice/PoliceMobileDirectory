const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Consolidated_V3_Expanded.xlsx');
const wb = xlsx.readFile(filePath);
const data = xlsx.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]]);

console.log('Checking ASP expansions:');
const aspRecords = data.filter(r => String(r.Rank).toLowerCase().includes('assistant superintendent of police'));
console.log(`Found ${aspRecords.length} records with Assistant Superintendent of Police.`);
console.table(aspRecords.slice(0, 5).map(r => ({Name: r.Name, Rank: r.Rank})));

const rawAsp = data.filter(r => r.Rank === 'ASP');
console.log(`Found ${rawAsp.length} records still as 'ASP'.`);
