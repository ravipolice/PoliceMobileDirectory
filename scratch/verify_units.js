const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);

const sheetName = 'Western Range – Mangaluru';
const sheet = wb.Sheets[sheetName];
if (sheet) {
    const data = xlsx.utils.sheet_to_json(sheet);
    console.log(`Sample Records in ${sheetName}:`);
    const samples = data.filter(r => 
        String(r.Name).includes('INT') || 
        String(r.Name).includes('ISD') || 
        String(r.Name).includes('(T)') || 
        String(r.Name).includes('KLA')
    ).slice(0, 10);
    console.table(samples.map(r => ({Name: r.Name, Unit: r.Unit, Station: r.Station})));
} else {
    console.log(`Sheet ${sheetName} not found.`);
}
