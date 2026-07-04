const xlsx = require('xlsx');
const path = require('path');

const csvPath = path.join(__dirname, '..', 'KSP_Officers_App.csv');
const wb = xlsx.readFile(csvPath);
const data = xlsx.utils.sheet_to_json(wb.Sheets[wb.SheetNames[0]]);

console.log('Total Rows in CSV:', data.length);
const distCounts = {};
data.forEach(r => distCounts[r.District] = (distCounts[r.District] || 0) + 1);
console.log('District Distribution in CSV:');
console.log(distCounts);
