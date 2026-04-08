const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise.xlsx';
const wb = xlsx.readFile(filePath, { cellText: false, cellDates: true });

// Read the first sheet
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];

// Parse to JSON, headers are on row 2 (index 1)
const data = xlsx.utils.sheet_to_json(sheet, { range: 1, raw: false });

const stdMap = {
    'Bengaluru': '080',
    'Bengaluru City': '080',
    'Bangalore City': '080',
    'Bengaluru Urban': '080',
    'Bengaluru Rural': '080',
    'Ramanagara': '080',
    'Mysuru': '0821',
    'Mysuru City': '0821',
    'Mangaluru': '0824',
    'Mangaluru City': '0824',
    'Dakshina Kannada': '0824',
    'Hubballi-Dharwad City': '0836',
    'Hubballi–Dharwad City': '0836',
    'Dharwad': '0836',
    'Belagavi': '0831',
    'Belagavi City': '0831',
    'Kalaburagi': '08472',
    'Kalaburagi City': '08472',
    '6th Bn – Kalaburagi': '08472',
    'Ballari': '08392',
    'Vijayanagara': '08394',
    'Tumakuru': '0816',
    'Tumkur': '0816',
    'Shivamogga': '08182',
    'Davanagere': '08192',
    'Udupi': '0820',
    'Hassan': '08172',
    'Chikkamagaluru': '08262',
    'Kodagu': '08272',
    'Mandya': '08232',
    'Chitradurga': '08194',
    'Raichur': '08532',
    'Bidar': '08482',
    'Vijayapura': '08352',
    'Bagalkot': '08354',
    'Bagalkote': '08354',
    'Gadag': '08372',
    'Haveri': '08375',
    'Koppal': '08539',
    'Uttara Kannada': '08382',
    'Kolar': '08152',
    'KGF': '08153',
    'Chikkaballapura': '08156',
    'Chamarajanagara': '08226',
    'Yadgiri': '08473'
};

const normalizeRange = (rangeStr) => {
    if (!rangeStr) return rangeStr;
    const s = rangeStr.trim();
    if (/north.*eastern/i.test(s)) return 'North-Eastern Range';
    if (/eastern/i.test(s) && !/north/i.test(s)) return 'Eastern Range';
    if (/central/i.test(s)) return 'Central Range';
    if (/northern/i.test(s)) return 'Northern Range';
    if (/southern/i.test(s)) return 'Southern Range';
    if (/western/i.test(s)) return 'Western Range';
    if (/ballari/i.test(s)) return 'Ballari Range';
    return s;
};

const normalizeCommissionerate = (distStr) => {
    if (!distStr) return distStr;
    const s = distStr.trim();
    if (/bangalore city/i.test(s)) return 'Bengaluru City';
    if (/hubli.*dharwad/i.test(s) || /hubballi.*dharwad/i.test(s)) return 'Hubballi-Dharwad City';
    return s;
};

let stdAddedCount = 0;

data.forEach(row => {
    // 1. Normalize Range
    if (row.Range) {
        row.Range = normalizeRange(row.Range);
    }
    
    // 2. Normalize Commissionerates and Sections
    if (row.Section === 'Commissionerates') {
        row.Section = 'Commissionerate';
    }
    if (row.District) {
        row.District = normalizeCommissionerate(row.District);
    }

    // Determine default STD code for the district
    const stdCode = stdMap[row.District] || '080'; // Fallback to 080 if unknown

    // 3. Map STD codes for landlines
    ['Office 1', 'Office 2'].forEach(key => {
        if (row[key]) {
            let num = String(row[key]).trim();
            // If it's a valid landline but doesn't have STD code
            // (Assumes landlines without STD are usually 6-8 digits long, or missing leading zero)
            // Sometimes it might just be the number. If it doesn't start with '0', we should prepend STD.
            if (!num.startsWith('0') && num.length >= 6 && num.length <= 8) {
                // Ensure there's a hyphen separator
                row[key] = `${stdCode}-${num}`;
                stdAddedCount++;
            } else if (num.startsWith('2') && num.length === 8 && stdCode === '080') {
                // Special case for Bangalore 8 digit numbers starting with 2
                row[key] = `${stdCode}-${num}`;
                stdAddedCount++;
            } else if (num.length >= 6 && num.length <= 8 && !num.includes('-')) {
                 row[key] = `${stdCode}-${num}`;
                 stdAddedCount++;
            }
        }
    });
});

console.log(`Added STD codes to ${stdAddedCount} landline numbers.`);

// Re-create the workbook with the title row
const wsData = [
    ['KSP Contact Directory - All Units (3791 records)']
];

// Get all unique headers from all rows to ensure no columns are lost
const headerSet = new Set();
data.forEach(row => {
    Object.keys(row).forEach(k => headerSet.add(k));
});

// Ensure the original order if possible, or just use the set
const expectedHeaders = ['Section', 'Unit', 'Range', 'District', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email'];
// Fallback to expected headers, but include any extra found
const headers = [...new Set([...expectedHeaders, ...Array.from(headerSet)])];

wsData.push(headers);

// Add rows
data.forEach(row => {
    const rowArr = headers.map(h => row[h]);
    wsData.push(rowArr);
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);

// Set some column widths
const wscols = [
    {wch: 25}, {wch: 20}, {wch: 20}, {wch: 20}, 
    {wch: 30}, {wch: 20}, {wch: 25}, {wch: 15}, 
    {wch: 15}, {wch: 15}, {wch: 15}, {wch: 35}
];
newSheet['!cols'] = wscols;

// Replace sheet
wb.Sheets[sheetName] = newSheet;

// Write to file
const outPath = '../KSP_Contacts_UnitWise_Updated.xlsx';
xlsx.writeFile(wb, outPath);
console.log('Successfully saved to KSP_Contacts_UnitWise_Updated.xlsx');
