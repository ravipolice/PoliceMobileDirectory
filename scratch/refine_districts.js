const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = 'ALL';
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

const districtKeywords = [
    'bellari', 'ballari', 'madikeri', 'gokak', 'kgf', 'madhugiri', 'mysuru', 
    'belagavi', 'mangaluru', 'kalaburagi', 'dwd', 'dharwad', 'hubballi',
    'bidar', 'raichur', 'koppal', 'gadag', 'haveri', 'bagalkot', 'vijayapura',
    'yadgir', 'chitradurga', 'davangere', 'shimoga', 'shivamogga', 'tumkur',
    'tumakuru', 'kolar', 'chikkaballapur', 'ramanagara', 'mandya', 'chamarajanagar',
    'hassan', 'udupi', 'kodagu', 'uttara kannada', 'karwar', 'vijayanagara'
];

let updatedCount = 0;

data.forEach(row => {
    if (row.Range === 'State Level' && (row.Unit === 'HQ' || row.Unit === 'Districts')) {
        const name = (row.Name || '').toLowerCase();
        
        // If it's Intelligence and contains a district name, move to Districts
        const hasDistrict = districtKeywords.some(k => name.includes(k));
        
        if (hasDistrict && !name.includes('admin') && !name.includes('chief')) {
            if (row.Unit !== 'Districts') {
                row.Unit = 'Districts';
                updatedCount++;
            }
        }
    }
});

console.log(`Refined ${updatedCount} more records into 'Districts' based on city/district keywords.`);

const headers = ['agid', 'Section', 'Unit', 'Range', 'District', 'Sub Division', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2', 'searchBlob'];
const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h] || ''));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map(() => ({wch: 25}));
wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);

console.log('Successfully updated Excel. Refreshing everything...');
