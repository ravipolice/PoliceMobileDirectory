const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

console.log('--- COMPREHENSIVE DIRECTORY AUDIT ---');
console.log(`Total Records: ${data.length}`);

// 1. Wing Check
const wings = [...new Set(data.map(r => r.Wing))];
console.log(`\nDetected Wings (${wings.length}):`, wings.join(', '));

// 2. Range Check
const blankRangeCount = data.filter(r => !r.Range && (r.Wing === 'L&O' || r.Wing === 'RANGES')).length;
console.log(`\nBlank Ranges for L&O/District records: ${blankRangeCount}`);

// 3. Sub Division Check
const subDivCount = data.filter(r => r['Sub Division']).length;
console.log(`Records with Sub Division: ${subDivCount}`);

// 4. Phone/STD Check
const landlinesWithoutSTD = data.filter(r => r['Office 1'] && !r['Office 1'].includes('-')).length;
console.log(`Landlines without STD prefix: ${landlinesWithoutSTD}`);

// 5. Special Unit Check
const cidCount = data.filter(r => r.Wing === 'CID').length;
const isdCount = data.filter(r => r.Wing === 'ISD').length;
const hgcdCount = data.filter(r => r.Wing === 'HG & CD').length;
console.log(`\nSpecialized Unit Counts:`);
console.log(`- CID: ${cidCount}`);
console.log(`- ISD: ${isdCount}`);
console.log(`- HG & CD: ${hgcdCount}`);

// 6. Section (Work Type) Check
const uniqueSections = [...new Set(data.map(r => r.Section))].length;
console.log(`\nUnique Sections (Work Types): ${uniqueSections}`);

console.log('\n--- SAMPLE OF DATA QUALITY (RANGE REDUNDANCY CHECK) ---');
const redundant = data.filter(r => r.Range && r.Range.toLowerCase().includes('range range'));
console.log(`Count of "Range Range" occurrences: ${redundant.length}`);
if (redundant.length > 0) {
    console.table(redundant.slice(0, 10).map(r => ({ Range: r.Range, Name: r.Name })));
}
