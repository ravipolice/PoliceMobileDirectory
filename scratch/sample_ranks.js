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

console.log('Sample PI records:');
const piRecords = allData.filter(r => r.Rank === 'PI').slice(0, 10);
console.table(piRecords.map(r => ({Name: r.Name, Rank: r.Rank, Station: r.Station})));
