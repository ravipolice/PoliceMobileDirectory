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

console.log('Sample Wireless Records:');
const wireless = allData.filter(r => String(r.Unit).includes('Wireless')).slice(0, 5);
console.table(wireless.map(r => ({Name: r.Name, Unit: r.Unit, Station: r.Station})));

console.log('\nSample Women PS Records:');
const women = allData.filter(r => String(r.Unit).includes('Women PS')).slice(0, 5);
console.table(women.map(r => ({Name: r.Name, Unit: r.Unit, Station: r.Station})));
