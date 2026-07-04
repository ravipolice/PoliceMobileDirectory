const fs = require('fs');
const path = require('path');

const jsonPath = path.join(__dirname, '../officers_merge_ready.json');
const rawData = fs.readFileSync(jsonPath, 'utf8');
const officers = JSON.parse(rawData);

let updatedCount = 0;

officers.forEach(officer => {
    const name = (officer.name || '').toUpperCase();
    const unit = (officer.unit || '').toUpperCase();
    
    // 1. Fix CID Unit
    if (name.includes('CID') && !unit.includes('CID')) {
        officer.unit = 'CID';
        officer.district = 'CID HQ';
        updatedCount++;
    }
    
    // 2. Fix Intelligence Unit
    if (name.includes('INT') && !unit.includes('INTELLIGENCE')) {
        officer.unit = 'Intelligence';
        officer.district = 'Intelligence HQ';
        updatedCount++;
    }

    // 3. Fix KSRP Unit
    if (name.includes('KSRP') && !unit.includes('KSRP')) {
        officer.unit = 'KSRP';
        updatedCount++;
    }

    // 4. Standardize Range strings (replace en-dash with hyphen for consistency)
    if (officer.unit) officer.unit = officer.unit.replace('–', '-');
    if (officer.district) officer.district = officer.district.replace('–', '-');
});

fs.writeFileSync(jsonPath, JSON.stringify(officers, null, 2), 'utf8');
console.log(`Updated ${updatedCount} officer records for CID/INT/KSRP.`);
