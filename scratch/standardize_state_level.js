const fs = require('fs');

const DATA_FILE = 'officers_merge_ready.json';

function standardizeData() {
    if (!fs.existsSync(DATA_FILE)) {
        console.error('Data file not found');
        return;
    }

    const data = JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
    let modifiedCount = 0;

    const standardized = data.map(officer => {
        const name = (officer.name || "").toUpperCase();
        const unit = (officer.unit || "").toUpperCase();
        const district = (officer.district || "").toUpperCase();
        
        let changed = false;

        // CID Standardisation
        if (name.includes('CID') || unit.includes('CID') || district.includes('CID')) {
            if (officer.unit !== 'CID' || officer.district !== 'Bengaluru City') {
                officer.unit = 'CID';
                officer.district = 'Bengaluru City';
                officer.subDivision = '';
                changed = true;
            }
        }

        // Intelligence Standardisation (INT)
        else if (name.includes('INT.') || name.includes('INTELLIGENCE') || unit.includes('INT.') || unit.includes('INTELLIGENCE')) {
            if (officer.unit !== 'Intelligence' || officer.district !== 'Bengaluru City') {
                officer.unit = 'Intelligence';
                officer.district = 'Bengaluru City';
                officer.subDivision = '';
                changed = true;
            }
        }

        // KSRP Standardisation
        else if (name.includes('KSRP') || unit.includes('KSRP')) {
            if (officer.unit !== 'KSRP' || officer.district !== 'Bengaluru City') {
                officer.unit = 'KSRP';
                officer.district = 'Bengaluru City';
                officer.subDivision = '';
                changed = true;
            }
        }

        // ISD Standardisation
        else if (name.includes('ISD') || unit.includes('ISD')) {
            if (officer.unit !== 'ISD' || officer.district !== 'Bengaluru City') {
                officer.unit = 'ISD';
                officer.district = 'Bengaluru City';
                officer.subDivision = '';
                changed = true;
            }
        }

        if (changed) {
            modifiedCount++;
            // Update searchBlob
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

        return officer;
    });

    if (modifiedCount > 0) {
        fs.writeFileSync(DATA_FILE, JSON.stringify(standardized, null, 2));
        console.log(`Successfully updated ${modifiedCount} state-level records.`);
    } else {
        console.log('No records needed updating.');
    }
}

standardizeData();
