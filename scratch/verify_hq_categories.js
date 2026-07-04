const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(filePath);
const sheet = wb.Sheets[wb.SheetNames[0]];
const data = xlsx.utils.sheet_to_json(sheet);

const samples = [
    { label: 'Original: Headquarters', filter: (r) => r.Section === 'CHIEF OFFICE' },
    { label: 'Original: Control Rooms', filter: (r) => r.Section === 'STATE POLICE HEADQUARTERS' && r.Unit === 'C/Room' },
    { label: 'Original: State Deputation', filter: (r) => r.Section === 'STATE POLICE HEADQUARTERS' && r.Unit.includes('Deputation') },
    { label: 'Original: Retired Officers', filter: (r) => r.Section === 'STATE POLICE HEADQUARTERS' && r.Unit.includes('Retired') },
    { label: 'Original: Special Units', filter: (r) => r.Section === 'SPECIAL UNITS' }
];

samples.forEach(s => {
    const match = data.filter(s.filter);
    console.log(`--- ${s.label} (Count: ${match.length}) ---`);
    if (match.length > 0) {
        console.table(match.slice(0, 3).map(r => ({ Section: r.Section, Unit: r.Unit, Name: r.Name })));
    }
});
