const xlsx = require('xlsx');

const filePath = '../KSP_Contacts_UnitWise_Final.xlsx';
const wb = xlsx.readFile(filePath);
const sheetName = wb.SheetNames[0];
const sheet = wb.Sheets[sheetName];
const data = xlsx.utils.sheet_to_json(sheet);

const webContacts = [
    { Section: 'State Police Headquarters', Unit: 'Headquarters', Designation: 'DG & IGP, Karnataka State', Office: '080-22211803', Mobile: '9480800001', Email: 'police@ksp.gov.in' },
    { Section: 'State Police Headquarters', Unit: 'Headquarters', Designation: 'ADGP, Administration', Office: '080-22942114', Mobile: '9480800003', Email: 'adgpadmin@ksp.gov.in' },
    { Section: 'State Police Headquarters', Unit: 'Headquarters', Designation: 'IGP, Headquarters', Office: '080-22942106', Mobile: '9480800005', Email: 'igphq@ksp.gov.in' },
    { Section: 'State Police Headquarters', Unit: 'Headquarters', Designation: 'DIGP, Headquarters', Office: '080-22942105', Mobile: '9480800006', Email: 'digphq@ksp.gov.in' },
    { Section: 'CID', Unit: 'Criminal Investigation Department (CID)', Designation: 'DGP, CID', Office: '080-22204362', Mobile: '9480800021', Email: 'dgp-cid@ksp.gov.in' },
    { Section: 'CID', Unit: 'Criminal Investigation Department (CID)', Designation: 'ADGP, CID', Office: '080-22204363', Mobile: '9480800022', Email: 'adgp-cid@ksp.gov.in' },
    { Section: 'ISD', Unit: 'Internal Security Division (ISD)', Designation: 'ADGP, Internal Security Division', Office: '080-22004200', Mobile: '9480800023', Email: 'adgp-isd@ksp.gov.in' },
    { Section: 'KLA', Unit: 'Karnataka Lokayukta', Designation: 'ADGP, Karnataka Lokayukta', Office: '080-22353700', Mobile: '9480800024', Email: 'adgplok@ksp.gov.in' },
    { Section: 'Commissionerate', Unit: 'Bengaluru City', Designation: 'Commissioner of Police, Bengaluru City', Office: '080-22260222', Mobile: '9480801001', Email: 'compolbcp@ksp.gov.in' },
    { Section: 'Commissionerate', Unit: 'Mysuru City', Designation: 'Commissioner of Police, Mysuru City', Office: '0821-2418300', Mobile: '9480802201', Email: 'compolmyc@ksp.gov.in' },
    { Section: 'Commissionerate', Unit: 'Hubballi-Dharwad City', Designation: 'Commissioner of Police, Hubballi-Dharwad', Office: '0836-2233500', Mobile: '9480802001', Email: 'compolhdc@ksp.gov.in' },
    { Section: 'Ranges', Unit: 'Central Range', Designation: 'IGP, Central Range (Bengaluru)', Office: '080-22264412', Mobile: '9480800027', Email: 'igpcr@ksp.gov.in' },
    { Section: 'Ranges', Unit: 'Western Range', Designation: 'IGP, Western Range (Mangaluru)', Office: '0824-2220501', Mobile: '9480800032', Email: 'igpwr@ksp.gov.in' },
    { Section: 'Ranges', Unit: 'Southern Range', Designation: 'IGP, Southern Range (Mysuru)', Office: '0821-2421200', Mobile: '9480800031', Email: 'igpsr@ksp.gov.in' }
];

let updatedCount = 0;
let newCount = 0;

webContacts.forEach(web => {
    // Fuzzy match: check if Name contains keywords from Designation
    // Or if Section/Unit match and it's a high level role
    let found = false;
    for (const row of data) {
        if (row.Section === web.Section && (row.Name && (row.Name.includes(web.Designation.split(',')[0]) || web.Designation.includes(row.Name)))) {
            row['Office 1'] = web.Office;
            row['Mobile 1'] = web.Mobile;
            row.Email = web.Email;
            if (row.Section === 'CID') row.District = 'Bengaluru City';
            found = true;
            updatedCount++;
            break;
        }
    }
    
    if (!found) {
        // Add as a new record at the top of its section
        const newRow = {
            Section: web.Section,
            Unit: web.Unit,
            Range: web.Section === 'Ranges' ? web.Unit : 'State Level',
            District: web.Section === 'CID' ? 'Bengaluru City' : (web.Section === 'Commissionerate' ? web.Unit : 'Bengaluru City'),
            Name: web.Designation,
            Rank: web.Designation.split(',')[0],
            Station: web.Designation.split(',')[1] || web.Unit,
            'Office 1': web.Office,
            'Mobile 1': web.Mobile,
            Email: web.Email
        };
        data.unshift(newRow);
        newCount++;
    }
});

console.log(`Updated ${updatedCount} existing records and added ${newCount} new records from 2025 data.`);

const headers = ['Section', 'Unit', 'Range', 'District', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email', 'Email2'];
const wsData = [headers];
data.forEach(row => {
    wsData.push(headers.map(h => row[h] || ''));
});

const newSheet = xlsx.utils.aoa_to_sheet(wsData);
newSheet['!cols'] = headers.map((h, i) => ({wch: [20, 20, 20, 20, 40, 20, 30, 15, 15, 15, 15, 35, 35][i]}));

wb.Sheets[sheetName] = newSheet;
xlsx.writeFile(wb, filePath);
console.log(`Successfully updated ${filePath} with 2025 directory data.`);
