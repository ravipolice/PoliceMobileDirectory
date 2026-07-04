const fs = require('fs');
const path = require('path');

const districtsPath = path.join(__dirname, '../../nandija/allDistrictsDump.json');
const districts = JSON.parse(fs.readFileSync(districtsPath, 'utf8'));

const activeDistricts = districts.filter(d => d.isActive !== false);

const commissionerateCities = [];
const battalions = [];
const ranges = [];
const hqItems = [];
const standardDistricts = [];

activeDistricts.forEach(d => {
    const name = d.name.trim();
    const upperName = name.toUpperCase();
    
    if (upperName.endsWith(" CITY")) {
        commissionerateCities.push(name);
    } else if (upperName.includes("BN") || upperName.includes("IRB") || upperName.includes("BATTALION")) {
        battalions.push(name);
    } else if (upperName.includes("RANGE")) {
        ranges.push(name);
    } else if (upperName === "HQ" || upperName === "STATE INT" || upperName === "UNIT_HQ" || upperName === "UNIT HQ" || upperName === "STATE LEVEL") {
        hqItems.push(name);
    } else {
        standardDistricts.push(name);
    }
});

console.log(`Total active districts: ${activeDistricts.length}`);
console.log(`\nCommissionerate Cities (${commissionerateCities.length}):`, commissionerateCities);
console.log(`\nBattalions (${battalions.length}):`, battalions);
console.log(`\nRanges (${ranges.length}):`, ranges);
console.log(`\nHQ Items (${hqItems.length}):`, hqItems);
console.log(`\nStandard Districts (${standardDistricts.length}):`, standardDistricts);
