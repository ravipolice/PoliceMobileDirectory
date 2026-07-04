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

console.log('Sample Functional Units Verification:');
const samples = allData.filter(r => 
    String(r.Name).includes('CPI') || 
    String(r.Name).includes('RFSL') || 
    String(r.Name).includes('C/Room') ||
    String(r.Name).includes('Home Guard')
).slice(0, 15);
console.table(samples.map(r => ({Name: r.Name, Unit: r.Unit})));
