const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath, { cellText: false, cellDates: true });

const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];

const data = xlsx.utils.sheet_to_json(sheet);

const districtMap = {
    'Bengaluru': 'Bengaluru City',
    'Bangalore': 'Bengaluru City',
    'Mysuru': 'Mysuru',
    'Mysore': 'Mysuru',
    'Mangaluru': 'Mangaluru',
    'Mangalore': 'Mangaluru',
    'Belagavi': 'Belagavi',
    'Belgaum': 'Belagavi',
    'Kalaburagi': 'Kalaburagi',
    'Gulbarga': 'Kalaburagi',
    'Hubballi': 'Hubballi-Dharwad City',
    'Hubli': 'Hubballi-Dharwad City',
    'Dharwad': 'Dharwad',
    'Ballari': 'Ballari',
    'Bellary': 'Ballari',
    'Bagalkote': 'Bagalkote',
    'Bagalkot': 'Bagalkote',
    'Bidar': 'Bidar',
    'Chamarajanagara': 'Chamarajanagara',
    'Chamarajanagar': 'Chamarajanagara',
    'Chikkaballapura': 'Chikkaballapura',
    'Chikkaballapur': 'Chikkaballapura',
    'Chikballapur': 'Chikkaballapura',
    'Chikkamagaluru': 'Chikkamagaluru',
    'Chikkamagalur': 'Chikkamagaluru',
    'Chikmagalur': 'Chikkamagaluru',
    'Chitradurga': 'Chitradurga',
    'Dakshina Kannada': 'Dakshina Kannada',
    'Davanagere': 'Davanagere',
    'Gadag': 'Gadag',
    'Hassan': 'Hassan',
    'Haveri': 'Haveri',
    'Kodagu': 'Kodagu',
    'Kolar': 'Kolar',
    'Kolara': 'Kolar',
    'Koppal': 'Koppal',
    'Mandya': 'Mandya',
    'Raichur': 'Raichur',
    'Ramanagara': 'Ramanagara',
    'Ramanagar': 'Ramanagara',
    'Shivamogga': 'Shivamogga',
    'Shimoga': 'Shivamogga',
    'Tumakuru': 'Tumakuru',
    'Tumkur': 'Tumakuru',
    'Udupi': 'Udupi',
    'Uttara Kannada': 'Uttara Kannada',
    'Vijayapura': 'Vijayapura',
    'Bijapur': 'Vijayapura',
    'Yadgiri': 'Yadgiri',
    'Yadgir': 'Yadgiri'
};

const keys = Object.keys(districtMap);
let fixedCount = 0;

data.forEach(row => {
    // Only try to fill if District is missing or generic
    if (!row.District || row.District === 'UNKNOWN' || row.District === 'State Level') {
        if (row.Name) {
            // Sort keys by length descending to match longer names first (e.g., 'Bengaluru City' before 'Bengaluru')
            // Actually, my map has short keys. I should iterate and find the best match.
            for (const key of keys) {
                // Use word boundary to avoid partial matches (e.g., 'Kolar' in 'Kolara')
                const regex = new RegExp('\\b' + key + '\\b', 'i');
                if (regex.test(row.Name)) {
                    row.District = districtMap[key];
                    fixedCount++;
                    break;
                }
            }
        }
    }
});

console.log(`Updated District for ${fixedCount} records based on Name field.`);

// Get headers from first row of data to maintain consistency
const headers = Object.keys(data[0]);

const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h]));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);

// Maintain column widths if possible (simplified here)
newSheet['!cols'] = [
    {wch: 25}, {wch: 20}, {wch: 20}, {wch: 20}, 
    {wch: 30}, {wch: 20}, {wch: 25}, {wch: 15}, 
    {wch: 15}, {wch: 15}, {wch: 15}, {wch: 35}, {wch: 35}
];

wb.Sheets[sheetName] = newSheet;

xlsx.writeFile(wb, filePath);
console.log(`Successfully updated ${filePath}`);
