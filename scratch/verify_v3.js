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

console.log('Sample PSI-1 Records:');
const psi1 = allData.filter(r => String(r.Rank).startsWith('PSI-1')).slice(0, 5);
console.table(psi1.map(r => ({Name: r.Name, Rank: r.Rank, Station: r.Station})));
