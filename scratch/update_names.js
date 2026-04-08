const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise.xlsx';
const wb = xlsx.readFile(filePath, { cellText: false, cellDates: true });

const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];

const data = xlsx.utils.sheet_to_json(sheet, { range: 1, raw: false });

const headerSet = new Set();
data.forEach(row => {
    Object.keys(row).forEach(k => headerSet.add(k));
});
const expectedHeaders = ['Section', 'Unit', 'Range', 'District', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email'];
const headers = [...new Set([...expectedHeaders, ...Array.from(headerSet)])];

let updatedCount = 0;

data.forEach(row => {
    let rankStr = row.Rank ? String(row.Rank).trim() : '';
    let stationStr = row.Station ? String(row.Station).trim() : '';

    if (rankStr || stationStr) {
        let isSpecial = (row.Section === 'Special Unit' || row.Section === 'State Police Headquarters' || row.Section === 'Ranges');
        
        if (isSpecial && stationStr) {
            // Remove "PS" or "Police Station" from Special Units
            stationStr = stationStr.replace(/\bPS\b/ig, '').replace(/\bPolice Station\b/ig, '').trim();
            // Remove trailing commas or hyphens if any left
            stationStr = stationStr.replace(/^[\s,-]+|[\s,-]+$/g, '');
        }
        
        row.Name = `${rankStr} ${stationStr}`.trim();
        updatedCount++;
    }
});

console.log(`Updated Name field for ${updatedCount} records.`);

const wsData = [
    ['KSP Contact Directory - All Units (3791 records)']
];
wsData.push(headers);

data.forEach(row => {
    const rowArr = headers.map(h => row[h]);
    wsData.push(rowArr);
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);

const wscols = [
    {wch: 25}, {wch: 20}, {wch: 20}, {wch: 20}, 
    {wch: 30}, {wch: 20}, {wch: 25}, {wch: 15}, 
    {wch: 15}, {wch: 15}, {wch: 15}, {wch: 35}
];
newSheet['!cols'] = wscols;

wb.Sheets[sheetName] = newSheet;

const outPath = '../KSP_Contacts_UnitWise.xlsx';
xlsx.writeFile(wb, outPath);
console.log(`Successfully updated ${outPath}`);
