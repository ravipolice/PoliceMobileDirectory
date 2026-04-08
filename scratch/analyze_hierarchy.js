const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets['ALL'];
const data = xlsx.utils.sheet_to_json(sheet);

const hierarchy = {};

data.forEach(row => {
    const s = row.Section || 'Unknown Section';
    const u = row.Unit || 'Unknown Unit';
    
    if (!hierarchy[s]) {
        hierarchy[s] = new Set();
    }
    hierarchy[s].add(u);
});

console.log('--- DATA HIERARCHY ANALYSIS ---');
console.log('Section (Broad Category) -> Units (Specific Branches)');
console.log('----------------------------------------------------');

Object.keys(hierarchy).sort().forEach(section => {
    const units = Array.from(hierarchy[section]).sort();
    console.log(`[${section}]`);
    if (units.length > 10) {
        console.log(`  - Includes ${units.length} units (e.g., ${units.slice(0, 5).join(', ')}...)`);
    } else {
        units.forEach(u => console.log(`  - ${u}`));
    }
});

// Find rows where Section and Unit are the same
const same = data.filter(r => r.Section === r.Unit).length;
console.log('\nNotes:');
console.log(`- In ${same} records, Section and Unit are the same (usually for top-level HQ or standalone units).`);
console.log('- Sections are used for the main menu/tabs in the app.');
console.log('- Units are used for filtering within a Section.');
