const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

const keywordMap = {
    'KSRP': 'KSRP',
    'Crime': 'Crime',
    'CTRS': 'Communication & Transport',
    'C&TS': 'Communication & Transport',
    'ANTF': 'ANTF',
    'Railways': 'Railways',
    'L&O': 'Law & Order',
    'Intelligence': 'Intelligence',
    'KSISF': 'KSISF',
    'Grievances': 'Grievances',
    'Law': 'Legal',
    'Admin': 'Administration',
    'MTO': 'Motor Transport',
    'PRO': 'Public Relations',
    'ISD': 'ISD',
    'C/Room': 'Control Room',
    'Control Room': 'Control Room',
    'Wireless': 'Wireless',
    'FSL': 'Forensic',
    'Recruitment': 'Recruitment',
    'Training': 'Training',
    'SCRB': 'SCRB (Computer)',
    'Computer': 'SCRB (Computer)'
};

const keys = Object.keys(keywordMap).sort((a, b) => b.length - a.length);

let sectionUpdated = 0;
let croomRenamed = 0;

data.forEach(row => {
    // 1. Rename any remaining 'Control Room' to 'C/Room' for consistency
    const fields = ['Section', 'Unit', 'Range', 'District', 'Name', 'Rank', 'Station'];
    fields.forEach(f => {
        if (row[f] && typeof row[f] === 'string' && row[f].includes('Control Room')) {
            row[f] = row[f].replace(/Control Room/gi, 'C/Room');
            croomRenamed++;
        }
    });

    // 2. Granular split for generic Headquarters
    if (row.Section === 'State Police Headquarters' || row.Unit === 'Headquarters' || row.Unit === 'C/Room') {
        if (row.Name) {
            for (const key of keys) {
                const regex = new RegExp(`\\b${key.replace('/', '\\/')}\\b`, 'i');
                if (regex.test(row.Name)) {
                    const newSection = keywordMap[key];
                    if (row.Section !== newSection) {
                        row.Section = newSection;
                        if (row.Unit === 'Headquarters' || row.Unit === 'C/Room') {
                            row.Unit = newSection;
                        }
                        sectionUpdated++;
                    }
                    break;
                }
            }
        }
    }
});

console.log(`Reclassified ${sectionUpdated} HQ records.`);
console.log(`Renamed ${croomRenamed} remaining "Control Room" instances to "C/Room".`);

const headers = ['agid', 'Section', 'Unit', 'Range', 'District', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2', 'searchBlob'];
const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h] || ''));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map(() => ({wch: 25}));
wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);
console.log(`Successfully updated ${filePath}`);
