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

console.log('Sample CAR Records:');
const car = allData.filter(r => String(r.Unit) === 'CAR').slice(0, 5);
console.table(car.map(r => ({Name: r.Name, Unit: r.Unit})));

console.log('\nSample DAR Records:');
const dar = allData.filter(r => String(r.Unit) === 'DAR').slice(0, 5);
console.table(dar.map(r => ({Name: r.Name, Unit: r.Unit})));
