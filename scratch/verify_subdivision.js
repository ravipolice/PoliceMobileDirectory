const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

console.log('--- Verification of Sub Divisions (Directional) ---');
const sample = data.filter(r => r['Sub Division'] && ['West', 'North', 'East', 'South', 'Whitefield'].includes(r['Sub Division'])).slice(0, 15);

console.table(sample.map(r => ({
    SubDivision: r['Sub Division'],
    Section: r.Section,
    Station: r.Station,
    Name: r.Name
})));
