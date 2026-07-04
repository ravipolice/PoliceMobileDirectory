const fs = require('fs');

const DATA_FILE = 'officers_merge_ready.json';

const CITY_TO_DISTRICT = {
    'MYSURU': 'Mysuru City',
    'MYSORE': 'Mysuru City',
    'BELAGAVI': 'Belagavi Dist',
    'BELGAUM': 'Belagavi Dist',
    'MANGALURU': 'Mangaluru City',
    'MANGALORE': 'Mangaluru City',
    'HUBBALLI': 'Hubballi-Dharwad City',
    'HUBLI': 'Hubballi-Dharwad City',
    'DHARWAD': 'Hubballi-Dharwad City',
    'KALABURAGI': 'Kalaburagi City',
    'GULBARGA': 'Kalaburagi City',
    'DAVANAGERE': 'Davanagere',
    'BALLARI': 'Ballari',
    'BELLARY': 'Ballari',
    'RAICHUR': 'Raichur',
    'HASSAN': 'Hassan',
    'SHIVAMOGGA': 'Shivamogga',
    'SHIMOGA': 'Shivamogga',
    'UDUPI': 'Udupi',
    'KARWAR': 'Uttara Kannada',
    'BIDAR': 'Bidar',
    'YADGIR': 'Yadgir',
    'KOPPAL': 'Koppal',
    'GADAG': 'Gadag',
    'HAVERI': 'Haveri',
    'BAGALKOT': 'Bagalkot',
    'VIJAYAPURA': 'Vijayapura',
    'BIJAPUR': 'Vijayapura',
    'CHITRADURGA': 'Chitradurga',
    'TUMAKURU': 'Tumakuru',
    'TUMKUR': 'Tumakuru',
    'KOLAR': 'Kolar',
    'CHIKKABALLAPURA': 'Chikkaballapura',
    'RAMANAGARA': 'Ramanagara',
    'CHAMARAJANAGAR': 'Chamarajanagar',
    'KODAGU': 'Kodagu',
    'CHIKKAMAGALURU': 'Chikkamagaluru',
    'DAKSHINA KANNADA': 'Dakshina Kannada',
    'VIJAYANAGARA': 'Vijayanagara'
};

function restoreIntelligence() {
    if (!fs.existsSync(DATA_FILE)) return;

    const data = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
    let restoredCount = 0;

    const updated = data.map(officer => {
        const name = (officer.name || "").toUpperCase();
        const unit = (officer.unit || "").toUpperCase();
        
        // We only restore Intelligence/INT/KSRP/ISD. 
        // CID stays in Bengaluru as per previous request.
        if (unit.includes('CID') || name.includes('CID')) {
            return officer; 
        }

        if (unit.includes('INTELLIGENCE') || unit.includes('INT.') || unit.includes('STATE INT') || 
            unit.includes('KSRP') || unit.includes('ISD')) {
            
            let foundDistrict = null;
            for (const [city, district] of Object.entries(CITY_TO_DISTRICT)) {
                if (name.includes(city) || (officer.station || "").toUpperCase().includes(city)) {
                    foundDistrict = district;
                    break;
                }
            }

            if (foundDistrict && officer.district !== foundDistrict) {
                officer.district = foundDistrict;
                restoredCount++;
                
                // Regenerate searchBlob
                const searchTerms = [
                    officer.name,
                    officer.rank,
                    officer.unit,
                    officer.district,
                    officer.subDivision,
                    officer.station,
                    officer.mobile,
                    officer.landline,
                    officer.email
                ].filter(Boolean).map(s => s.toString().toLowerCase());
                officer.searchBlob = searchTerms.join(' ');
            }
        }

        return officer;
    });

    if (restoredCount > 0) {
        fs.writeFileSync(DATA_FILE, JSON.stringify(updated, null, 2));
        console.log(`Restored ${restoredCount} records to their regional districts.`);
    } else {
        console.log('No records restored.');
    }
}

restoreIntelligence();
