const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = 'ALL';
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

const newContacts = [
  { "Battalion": "KSRP Headquarters", "Designation": "ADGP", "Mobile": "9480800009" },
  { "Battalion": "KSRP Headquarters", "Designation": "IGP", "Mobile": "9480800025" },
  { "Battalion": "KSRP Headquarters", "Designation": "DIGP (Admin & Training)", "Mobile": "9480800052" },
  { "Battalion": "I Battalion (Bengaluru)", "Designation": "Commandant", "Mobile": "9480805801" },
  { "Battalion": "I Battalion (Bengaluru)", "Designation": "Dy. Commandant", "Mobile": "9480800828" },
  { "Battalion": "I Battalion (Bengaluru)", "Designation": "Asst. Commandant", "Mobile": "9480805828" },
  { "Battalion": "I Battalion (Bengaluru)", "Designation": "Asst. Commandant", "Mobile": "9480805826" },
  { "Battalion": "I Battalion (Bengaluru)", "Designation": "Asst. Commandant", "Mobile": "9480805827" },
  { "Battalion": "II Battalion (Belagavi)", "Designation": "Commandant", "Mobile": "9480805802" },
  { "Battalion": "II Battalion (Belagavi)", "Designation": "Dy. Commandant", "Mobile": "9480805829" },
  { "Battalion": "II Battalion (Belagavi)", "Designation": "Asst. Commandant", "Mobile": "9480805830" },
  { "Battalion": "II Battalion (Belagavi)", "Designation": "Asst. Commandant", "Mobile": "9480805831" },
  { "Battalion": "III Battalion (Bengaluru)", "Designation": "Commandant", "Mobile": "9480805803" },
  { "Battalion": "III Battalion (Bengaluru)", "Designation": "Dy. Commandant", "Mobile": "9480800838" },
  { "Battalion": "III Battalion (Bengaluru)", "Designation": "Asst. Commandant", "Mobile": "9480805832" },
  { "Battalion": "III Battalion (Bengaluru)", "Designation": "Asst. Commandant", "Mobile": "9480805833" },
  { "Battalion": "IV Battalion (Bengaluru)", "Designation": "Commandant", "Mobile": "9480805804" },
  { "Battalion": "IV Battalion (Bengaluru)", "Designation": "Dy. Commandant", "Mobile": "6360573813" },
  { "Battalion": "IV Battalion (Bengaluru)", "Designation": "Asst. Commandant", "Mobile": "9480805836" },
  { "Battalion": "IV Battalion (Bengaluru)", "Designation": "Asst. Commandant", "Mobile": "9480805837" },
  { "Battalion": "IV Battalion (Bengaluru)", "Designation": "Asst. Commandant", "Mobile": "9480805835" },
  { "Battalion": "V Battalion (Mysuru)", "Designation": "Commandant", "Mobile": "9480805805" },
  { "Battalion": "V Battalion (Mysuru)", "Designation": "Dy. Commandant", "Mobile": "9480805839" },
  { "Battalion": "V Battalion (Mysuru)", "Designation": "Asst. Commandant", "Mobile": "9480805838" },
  { "Battalion": "V Battalion (Mysuru)", "Designation": "Asst. Commandant", "Mobile": "9480805840" },
  { "Battalion": "VI Battalion (Kalaburagi)", "Designation": "Commandant", "Mobile": "9480805806" },
  { "Battalion": "VI Battalion (Kalaburagi)", "Designation": "Dy. Commandant", "Mobile": "9480805844" },
  { "Battalion": "VI Battalion (Kalaburagi)", "Designation": "Asst. Commandant", "Mobile": "9480805843" },
  { "Battalion": "VII Battalion (Mangaluru)", "Designation": "Commandant", "Mobile": "9480805807" },
  { "Battalion": "VII Battalion (Mangaluru)", "Designation": "Dy. Commandant", "Mobile": "9481967964" },
  { "Battalion": "VII Battalion (Mangaluru)", "Designation": "Asst. Commandant", "Mobile": "9480805845" },
  { "Battalion": "VII Battalion (Mangaluru)", "Designation": "Asst. Commandant", "Mobile": "9480805846" },
  { "Battalion": "VII Battalion (Mangaluru)", "Designation": "Asst. Commandant", "Mobile": "9480805847" },
  { "Battalion": "VIII Battalion (Shivamogga)", "Designation": "Commandant", "Mobile": "9480805808" },
  { "Battalion": "VIII Battalion (Shivamogga)", "Designation": "Dy. Commandant", "Mobile": "9480805849" },
  { "Battalion": "VIII Battalion (Shivamogga)", "Designation": "Asst. Commandant", "Mobile": "9480805851" },
  { "Battalion": "IX Battalion (Kudlu)", "Designation": "Commandant", "Mobile": "9480805809" },
  { "Battalion": "IX Battalion (Kudlu)", "Designation": "Dy. Commandant", "Mobile": "9480805854" },
  { "Battalion": "IX Battalion (Kudlu)", "Designation": "Asst. Commandant", "Mobile": "9480805853" },
  { "Battalion": "IX Battalion (Kudlu)", "Designation": "Asst. Commandant", "Mobile": "9480805857" },
  { "Battalion": "IX Battalion (Kudlu)", "Designation": "Asst. Commandant", "Mobile": "9480805856" },
  { "Battalion": "X Battalion (Shiggaon)", "Designation": "Commandant", "Mobile": "9480805810" },
  { "Battalion": "X Battalion (Shiggaon)", "Designation": "Dy. Commandant", "Mobile": "9480805858" },
  { "Battalion": "X Battalion (Shiggaon)", "Designation": "Asst. Commandant", "Mobile": "9480805859" },
  { "Battalion": "X Battalion (Shiggaon)", "Designation": "Asst. Commandant", "Mobile": "9480805860" },
  { "Battalion": "XI Battalion (Hassan)", "Designation": "Commandant", "Mobile": "9480805811" },
  { "Battalion": "XI Battalion (Hassan)", "Designation": "Dy. Commandant", "Mobile": "9480805818" },
  { "Battalion": "XI Battalion (Hassan)", "Designation": "Asst. Commandant", "Mobile": "9480805862" },
  { "Battalion": "XI Battalion (Hassan)", "Designation": "Asst. Commandant", "Mobile": "9480805865" },
  { "Battalion": "XII Battalion (Tumakuru)", "Designation": "Commandant", "Mobile": "9480800843" },
  { "Battalion": "IRB Munirabad", "Designation": "Commandant", "Mobile": "9480805812" },
  { "Battalion": "IRB Vijayapura", "Designation": "Commandant", "Mobile": "9480804280" }
];

let mergedCount = 0;
let newRecordsCount = 0;

newContacts.forEach(nc => {
    let matched = false;
    
    // Extract Battalion number (e.g. "I", "IV", "IRB")
    const bnMatch = nc.Battalion.match(/^([IVXLC]+|IRB)\s+Battalion/i) || [null, nc.Battalion];
    const bnId = bnMatch[1];
    
    data.forEach(row => {
        if (row.Section === 'KSRP' || row.Section === 'Karnataka State Reserve Police (KSRP)') {
            // Check if Name or Station contains the BN ID and the Designation matches
            const nameMatch = row.Name && row.Name.includes(bnId);
            const rankMatch = (row.Rank && row.Rank.includes(nc.Designation)) || (row.Name && row.Name.includes(nc.Designation));
            
            if (nameMatch && rankMatch) {
                if (!row['Mobile 1'] || row['Mobile 1'] === '') {
                    row['Mobile 1'] = nc.Mobile;
                    mergedCount++;
                }
                matched = true;
            }
        }
    });
    
    if (!matched) {
        // Add as new record
        data.push({
            'agid': 'KSP' + (data.length + 1).toString().padStart(4, '0'),
            'Section': 'KSRP',
            'Unit': 'KSRP Battalions',
            'Range': 'State Level',
            'District': 'KSRP',
            'Sub Division': nc.Battalion,
            'Name': nc.Designation + ' ' + nc.Battalion,
            'Rank': nc.Designation,
            'Station': nc.Battalion,
            'Mobile 1': nc.Mobile
        });
        newRecordsCount++;
    }
});

console.log(`Merged ${mergedCount} mobile numbers and added ${newRecordsCount} new KSRP contacts.`);

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
