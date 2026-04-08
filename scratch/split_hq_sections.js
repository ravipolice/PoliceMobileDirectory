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
    'ISD': 'ISD'
};

const keys = Object.keys(keywordMap).sort((a, b) => b.length - a.length);

let sectionUpdated = 0;

data.forEach(row => {
    // Only perform this granular split for generic Headquarters or State Level sections
    if (row.Section === 'State Police Headquarters' || row.Unit === 'Headquarters') {
        if (row.Name) {
            for (const key of keys) {
                const regex = new RegExp(`\\b${key}\\b`, 'i');
                if (regex.test(row.Name)) {
                    const newSection = keywordMap[key];
                    
                    // If we found a specific unit, update Section and Unit to be more descriptive
                    if (row.Section !== newSection) {
                        row.Section = newSection;
                        // Also update Unit if it's generic
                        if (row.Unit === 'Headquarters') {
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

console.log(`Reclassified ${sectionUpdated} HQ records into specific sections (Crime, KSRP, CTRS, etc.).`);

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
