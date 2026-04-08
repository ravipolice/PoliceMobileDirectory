const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = 'ALL';
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

const newContacts = [
  { "BN": "I", "Location": "Bengaluru", "Designation": "Commdt.", "Mobile": "9480805801" },
  { "BN": "I", "Location": "Bengaluru", "Designation": "Dy. Commdt.", "Mobile": "9480800828" },
  { "BN": "I", "Location": "Bengaluru", "Designation": "Asst. Commdt.", "Mobile": "9480805828" },
  { "BN": "I", "Location": "Bengaluru", "Designation": "Asst. Commdt.", "Mobile": "9480805826" },
  { "BN": "I", "Location": "Bengaluru", "Designation": "Asst. Commdt.", "Mobile": "9480805827" },
  { "BN": "II", "Location": "Belagavi", "Designation": "Commdt.", "Mobile": "9480805802" },
  { "BN": "II", "Location": "Belagavi", "Designation": "Dy. Commdt.", "Mobile": "9480805829" },
  { "BN": "II", "Location": "Belagavi", "Designation": "Asst. Commdt.", "Mobile": "9480805830" },
  { "BN": "II", "Location": "Belagavi", "Designation": "Asst. Commdt.", "Mobile": "9480805831" },
  { "BN": "III", "Location": "Bengaluru", "Designation": "Commdt.", "Mobile": "9480805803" },
  { "BN": "III", "Location": "Bengaluru", "Designation": "Dy. Commdt.", "Mobile": "9480800838" },
  { "BN": "III", "Location": "Bengaluru", "Designation": "Asst. Commdt.", "Mobile": "9480805832" },
  { "BN": "III", "Location": "Bengaluru", "Designation": "Asst. Commdt.", "Mobile": "9480805833" },
  { "BN": "IV", "Location": "Bengaluru", "Designation": "Commdt.", "Mobile": "9480805804" },
  { "BN": "IV", "Location": "Bengaluru", "Designation": "Dy. Commdt.", "Mobile": "6360573813" },
  { "BN": "IV", "Location": "Bengaluru", "Designation": "Asst. Commdt.", "Mobile": "9480805836" },
  { "BN": "IV", "Location": "Bengaluru", "Designation": "Asst. Commdt.", "Mobile": "9480805837" },
  { "BN": "V", "Location": "Mysuru", "Designation": "Commdt.", "Mobile": "9480805805" },
  { "BN": "VI", "Location": "Kalaburagi", "Designation": "Commdt.", "Mobile": "9480805806" },
  { "BN": "VII", "Location": "Mangaluru", "Designation": "Commdt.", "Mobile": "9480805807" },
  { "BN": "VIII", "Location": "Shivamogga", "Designation": "Commdt.", "Mobile": "9480805808" },
  { "BN": "IX", "Location": "Kudlu", "Designation": "Commdt.", "Mobile": "9480805809" },
  { "BN": "X", "Location": "Shiggaon", "Designation": "Commdt.", "Mobile": "9480805810" },
  { "BN": "XI", "Location": "Hassan", "Designation": "Commdt.", "Mobile": "9480805811" },
  { "BN": "XII", "Location": "Tumakuru", "Designation": "Commdt.", "Mobile": "9480800843" },
  { "BN": "IRB", "Location": "Munirabad", "Designation": "Commdt.", "Mobile": "9480805812" },
  { "BN": "IRB", "Location": "Vijayapura", "Designation": "Commdt.", "Mobile": "9480804280" }
];

let mergedCount = 0;
let newRecordsCount = 0;

newContacts.forEach(nc => {
    let matched = false;
    const bnName = nc.BN + ' BN';
    
    data.forEach(row => {
        if (row.Section === 'KSRP' || row.Section === 'Karnataka State Reserve Police (KSRP)') {
            const hasBN = row.Name && row.Name.includes(nc.BN);
            const hasRank = row.Name && row.Name.includes(nc.Designation);
            
            if (hasBN && hasRank) {
                if (!row['Mobile 1'] || row['Mobile 1'] === '') {
                    row['Mobile 1'] = nc.Mobile;
                    mergedCount++;
                }
                matched = true;
            }
        }
    });
    
    if (!matched) {
        data.push({
            'agid': 'KSP' + (data.length + 1).toString().padStart(4, '0'),
            'Section': 'KSRP',
            'Unit': 'KSRP Battalions',
            'Range': 'State Level',
            'District': 'KSRP',
            'Sub Division': bnName + ' (' + nc.Location + ')',
            'Name': nc.Designation + ' ' + bnName,
            'Rank': nc.Designation,
            'Station': bnName + ' ' + nc.Location,
            'Mobile 1': nc.Mobile
        });
        newRecordsCount++;
    }
});

console.log(`Merged ${mergedCount} mobile numbers and added ${newRecordsCount} new role-based KSRP contacts.`);

const headers = ['agid', 'Section', 'Unit', 'Range', 'District', 'Sub Division', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2', 'searchBlob'];
const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h] || ''));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map(() => ({wch: 25}));
wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);
console.log(`Successfully updated ${filePath}`);
