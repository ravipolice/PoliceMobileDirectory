const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];
let data = xlsx.utils.sheet_to_json(sheet);

// 1. Correct the Commissioner contacts (manually to be safe)
data.forEach(row => {
    if (row.Name === 'Commissioner of Police, Bengaluru City') {
        row['Office 1'] = '080-22260222';
        row['Mobile 1'] = '9480801001';
        row.Email = 'compolbcp@ksp.gov.in';
        row.Unit = 'Bengaluru City';
        row.District = 'Bengaluru City';
    }
    if (row.Name === 'Commissioner of Police, Hubballi-Dharwad') {
        row['Office 1'] = '0836-2233500';
        row['Mobile 1'] = '9480802001';
        row.Email = 'compolhdc@ksp.gov.in';
        row.Unit = 'Hubballi-Dharwad City';
        row.District = 'Dharwad';
    }
});

// 2. Cleanup and Pre-process for PMD App
// Generate AGID and searchBlob
data.forEach((row, index) => {
    // Generate AGID
    row.agid = `KSP${String(index + 1).padStart(4, '0')}`;
    
    // Clean fields
    const rank = (row.Rank || '').trim();
    const station = (row.Station || '').trim();
    const unit = (row.Unit || '').trim();
    const district = (row.District || '').trim();
    const subDivision = (row['Sub Division'] || '').trim();
    
    // ✅ Ensure Name is populated (use Rank + Station as fallback)
    if (!row.Name || row.Name.trim() === '') {
        if (rank && station) {
            row.Name = `${rank}, ${station}`;
        } else {
            row.Name = rank || station || 'Police Officer';
        }
    }
    const name = (row.Name || '').trim();
    
    // Generate searchBlob (concatenation of all relevant fields for easy search)
    const blobParts = [
        name, rank, station, unit, district, subDivision, row.Section, 
        row['Office 1'], row['Mobile 1'], row.Email
    ].filter(v => v && v.trim() !== '').map(v => String(v).toLowerCase());
    
    row.searchBlob = [...new Set(blobParts)].join(' ');
});

// 3. Save the corrected Excel
const headers = ['agid', 'Section', 'Unit', 'Range', 'District', 'Sub Division', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2', 'searchBlob'];
const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h] || ''));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map(() => ({wch: 25}));
wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);

// 4. Export to CSV for PMD App (Matching OfficerEntity schema)
const appCsvPath = '../KSP_Officers_App.csv';
const appData = data.map(r => ({
    agid: r.agid,
    name: r.Name,
    rank: r.Rank,
    station: r.Station,
    unit: r.Unit,
    district: r.District,
    subDivision: r['Sub Division'],
    landline: r['Office 1'],
    mobile: r['Mobile 1'],
    email: r.Email,
    searchBlob: r.searchBlob
}));

const appSheet = xlsx.utils.json_to_sheet(appData);
const csvContent = xlsx.utils.sheet_to_csv(appSheet);
require('fs').writeFileSync(appCsvPath, csvContent, 'utf8');

console.log(`Success! Updated ${filePath} and generated ${appCsvPath} for PMD App.`);
console.table(data.slice(0, 5).map(r => ({agid: r.agid, Name: r.Name, Mobile: r['Mobile 1']})));
