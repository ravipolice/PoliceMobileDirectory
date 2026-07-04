const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

const targets = ['VIP C/Room', 'Makkala Sahayavani', 'Elders Helpline', 'Namma 112'];
const results = data.filter(r => targets.some(t => r.Station && r.Station.includes(t)));

console.log('--- Verification of Comma-less Designations ---');
console.table(results.map(r => ({ Rank: r.Rank, Station: r.Station, Section: r.Section })));
