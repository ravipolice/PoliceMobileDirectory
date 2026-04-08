const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath, { cellText: false, cellDates: true });

const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];

const data = xlsx.utils.sheet_to_json(sheet);

const talukToDistrict = {
    // Tumakuru
    'Koratagere': 'Tumakuru', 'Madhugiri': 'Tumakuru', 'Pavagada': 'Tumakuru', 'Sira': 'Tumakuru', 
    'Tiptur': 'Tumakuru', 'Gubbi': 'Tumakuru', 'Kunigal': 'Tumakuru', 'Turuvekere': 'Tumakuru', 
    'Chikkanayakanahalli': 'Tumakuru', 'Tumkur': 'Tumakuru', 'Tumakuru': 'Tumakuru',
    
    // Ramanagara
    'Channapatna': 'Ramanagara', 'Kanakapura': 'Ramanagara', 'Magadi': 'Ramanagara', 'Ramanagar': 'Ramanagara', 'Ramanagara': 'Ramanagara',
    
    // Kolar
    'Bangarapet': 'Kolar', 'Malur': 'Kolar', 'Mulbagal': 'Kolar', 'Srinivaspur': 'Kolar', 'Kolara': 'Kolar', 'Kolar': 'Kolar',
    
    // Chikkaballapura
    'Bagepalli': 'Chikkaballapura', 'Chintamani': 'Chikkaballapura', 'Gauribidanur': 'Chikkaballapura', 
    'Gudibanda': 'Chikkaballapura', 'Sidlaghatta': 'Chikkaballapura', 'Chikkaballapur': 'Chikkaballapura', 'Chikkaballapura': 'Chikkaballapura', 'Chikballapur': 'Chikkaballapura',
    
    // Belagavi
    'Athani': 'Belagavi', 'Bailhongal': 'Belagavi', 'Chikkodi': 'Belagavi', 'Gokak': 'Belagavi', 
    'Hukkeri': 'Belagavi', 'Khanapur': 'Belagavi', 'Raybag': 'Belagavi', 'Ramdurg': 'Belagavi', 
    'Saundatti': 'Belagavi', 'Belgaum': 'Belagavi', 'Belagavi': 'Belagavi',
    
    // Mandya
    'Maddur': 'Mandya', 'Malavalli': 'Mandya', 'Pandavapura': 'Mandya', 'Srirangapatna': 'Mandya', 'Nagamangala': 'Mandya', 'Krishnarajapet': 'Mandya',
    
    // Hassan
    'Arsikere': 'Hassan', 'Channarayapatna': 'Hassan', 'Holenarasipura': 'Hassan', 'Sakleshpur': 'Hassan', 'Belur': 'Hassan', 'Arkalgud': 'Hassan',
    
    // Mysuru
    'Nanjangud': 'Mysuru', 'Hunsur': 'Mysuru', 'T.Narasipura': 'Mysuru', 'Periyapatna': 'Mysuru', 'Mysore': 'Mysuru', 'Mysuru': 'Mysuru',
    
    // Chikkamagaluru
    'Kadur': 'Chikkamagaluru', 'Koppa': 'Chikkamagaluru', 'Tarikere': 'Chikkamagaluru', 'Mudigere': 'Chikkamagaluru', 'Chikmagalur': 'Chikkamagaluru', 'Chikkamagaluru': 'Chikkamagaluru',
    
    // Davanagere
    'Harihara': 'Davanagere', 'Honnali': 'Davanagere', 'Jagalur': 'Davanagere', 'Channagiri': 'Davanagere',
    
    // Shivamogga
    'Bhadravathi': 'Shivamogga', 'Sagar': 'Shivamogga', 'Shikaripura': 'Shivamogga', 'Soraba': 'Shivamogga', 'Thirthahalli': 'Shivamogga', 'Shimoga': 'Shivamogga', 'Shivamogga': 'Shivamogga',
    
    // Ballari / Vijayanagara
    'Hospet': 'Vijayanagara', 'Sandur': 'Ballari', 'Siruguppa': 'Ballari', 'Bellary': 'Ballari', 'Ballari': 'Ballari',
    
    // Vijayapura
    'Indi': 'Vijayapura', 'Sindgi': 'Vijayapura', 'Muddebihal': 'Vijayapura', 'Basavana Bagewadi': 'Vijayapura', 'Bijapur': 'Vijayapura', 'Vijayapura': 'Vijayapura',
    
    // Bagalkote
    'Jamkhandi': 'Bagalkote', 'Mudhol': 'Bagalkote', 'Badami': 'Bagalkote', 'Hungund': 'Bagalkote', 'Bagalkot': 'Bagalkote', 'Bagalkote': 'Bagalkote',
    
    // Kalaburagi
    'Afzalpur': 'Kalaburagi', 'Aland': 'Kalaburagi', 'Chincholi': 'Kalaburagi', 'Chitapur': 'Kalaburagi', 'Sedam': 'Kalaburagi', 'Shahabad': 'Kalaburagi', 'Gulbarga': 'Kalaburagi', 'Kalaburagi': 'Kalaburagi',
    
    // Yadgiri
    'Shorapur': 'Yadgiri', 'Shahapur': 'Yadgiri', 'Yadgir': 'Yadgiri', 'Yadgiri': 'Yadgiri',
    
    // Bidar
    'Basavakalyan': 'Bidar', 'Bhalki': 'Bidar', 'Homnabad': 'Bidar', 'Aurad': 'Bidar',
    
    // Bengaluru
    'Bengaluru': 'Bengaluru City', 'Bangalore': 'Bengaluru City', 'Bengaluru Urban': 'Bengaluru City'
};

const talukKeys = Object.keys(talukToDistrict).sort((a, b) => b.length - a.length);

let updatedCount = 0;
let standardizedCount = 0;

data.forEach(row => {
    let originalDistrict = row.District;
    
    // 1. Try to fill/correct from Name field
    if (row.Name) {
        for (const taluk of talukKeys) {
            const regex = new RegExp('\\b' + taluk + '\\b', 'i');
            if (regex.test(row.Name)) {
                const targetDistrict = talukToDistrict[taluk];
                
                // If current district is missing, generic, or potentially wrong (e.g. 'Bengaluru Urban' vs 'Tumakuru')
                if (!row.District || row.District === 'UNKNOWN' || row.District === 'State Level' || 
                    row.District === 'Bengaluru Urban' || row.District === 'Bengaluru') {
                    
                    if (row.District !== targetDistrict) {
                        row.District = targetDistrict;
                        updatedCount++;
                    }
                }
                break;
            }
        }
    }
    
    // 2. Standardize existing District names even if no Name match
    if (row.District && talukToDistrict[row.District]) {
        const target = talukToDistrict[row.District];
        if (row.District !== target) {
            row.District = target;
            standardizedCount++;
        }
    }
});

console.log(`Updated District for ${updatedCount} records based on Name field.`);
console.log(`Standardized District for ${standardizedCount} existing records.`);

const headers = Object.keys(data[0]);
const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h]));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = [
    {wch: 25}, {wch: 20}, {wch: 20}, {wch: 20}, 
    {wch: 30}, {wch: 20}, {wch: 25}, {wch: 15}, 
    {wch: 15}, {wch: 15}, {wch: 15}, {wch: 35}, {wch: 35}
];

wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);
console.log(`Successfully updated ${filePath}`);
