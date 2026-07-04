const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);
const data = xlsx.utils.sheet_to_json(wbSource.Sheets['MASTER_MERGED']);

const rangeMap = {
    "Bagalkote": "Northern Range – Belagavi", "Belagavi": "Northern Range – Belagavi",
    "Dharwad": "Northern Range – Belagavi", "Gadag": "Northern Range – Belagavi", 
    "Vijayapura": "Northern Range – Belagavi",
    "Ballari": "Ballari Range – Ballari", "Raichur": "Ballari Range – Ballari", 
    "Koppal": "Ballari Range – Ballari", "Vijayanagara": "Ballari Range – Ballari",
    "Bidar": "North-Eastern Range – Kalaburag", "Kalaburagi": "North-Eastern Range – Kalaburag", 
    "Yadgir": "North-Eastern Range – Kalaburag",
    "Chamarajanagara": "Southern Range – Mysuru", "Hassan": "Southern Range – Mysuru", 
    "Kodagu": "Southern Range – Mysuru", "Mandya": "Southern Range – Mysuru", 
    "Mysuru": "Southern Range – Mysuru",
    "Chikkaballapura": "Central Range – Bengaluru", "Kolar": "Central Range – Bengaluru", 
    "Ramanagara": "Central Range – Bengaluru", "Tumakuru": "Central Range – Bengaluru",
    "Chitradurga": "Eastern Range – Davanagere", "Davanagere": "Eastern Range – Davanagere", 
    "Haveri": "Eastern Range – Davanagere",
    "Dakshina Kannada": "Western Range – Mangaluru", "Mangaluru": "Western Range – Mangaluru",
    "Udupi": "Western Range – Mangaluru", "Chikkamagaluru": "Western Range – Mangaluru", 
    "Shivamogga": "Western Range – Mangaluru", "Uttara Kannada": "Western Range – Mangaluru"
};

const distKeywords = {
    "Bagalkote": /Bagalkot|Bagalkote|Mudhol|Jamkhandi|Hunugund/i,
    "Gadag": /Gadag|Naragund|Ron|Mundargi|Shirhatti/i,
    "Vijayapura": /Vijayapura|Bijapur|Indi|Muddebihal|Sindagi/i,
    "Dharwad": /Dharwad|Navalgund|Kalghatgi|Alnavar/i,
    "Haveri": /Haveri|Ranebennur|Byadgi|Hanagal|Hirekerur/i,
    "Belagavi": /Belagavi|Belgaum|Chikkodi|Gokak|Athani|Bailhongal|Saundatti/i,
    "Ballari": /Ballari|Bellary|Siruguppa|Kampli|Sandur/i,
    "Vijayanagara": /Vijayanagara|Hospet|Hadagali|Hagaribommanahalli/i,
    "Raichur": /Raichur|Manvi|Sindhanur|Devadurga|Lingsugur/i,
    "Koppal": /Koppal|Gangavathi|Kushtagi|Yalaburga/i,
    "Bidar": /Bidar|Aurad|Basavakalyan|Bhalki|Humnabad/i,
    "Kalaburagi": /Kalaburagi|Gulbarga|Aland|Afzalpur|Chincholi|Sedam|Chittapur/i,
    "Yadgir": /Yadgir|Shorapur|Shahapur/i,
    "Shivamogga": /Shivamogga|Shimoga|Sagara|Shikaripura|Soraba|Thirthahalli/i,
    "Chikkamagaluru": /Chikkamagaluru|Chikmagalur|Kadur|Koppa|Mudigere|Tarikere/i,
    "Udupi": /Udupi|Karkala|Kundapura/i,
    "Uttara Kannada": /Uttara Kannada|Karwar|Sirsi|Kumta|Bhatkal|Haliyal/i,
    "Dakshina Kannada": /Dakshina Kannada|Mangaluru|Puttur|Belthangady|Sullia|Bantwal/i,
    "Tumakuru": /Tumakuru|Tumkur|Tiptur|Sira|Gubbi|Madhugiri|Pavagada/i,
    "Chikkaballapura": /Chikkaballapura|Chikballapur|Sidlaghatta|Chintamani|Gauribidanur/i,
    "Kolar": /Kolar|KGF|Mulbagal|Bangarapet|Malur/i,
    "Ramanagara": /Ramanagara|Channapatna|Kanakapura|Magadi/i,
    "Mandya": /Mandya|Maddur|Malavalli|Srirangapatna|Pandavapura|Nagamangala/i,
    "Hassan": /Hassan|Arasikere|Channarayapatna|Holenarasipura|Sakaleshapura/i,
    "Chamarajanagara": /Chamarajanagara|Chamarajanagar|Kollegala|Gundlupet/i,
    "Kodagu": /Kodagu|Madikeri|Virajpet|Somwarpet/i,
    "Mysuru": /Mysuru|Mysore|Hunsur|Nanjanagudu|T Narasipura|Periyapatna/i,
    "Davanagere": /Davanagere|Davangere|Harihara|Honnali|Jagalur/i,
    "Chitradurga": /Chitradurga|Hiriyur|Hosadurga|Holalkere|Molakalmuru/i
};

const specialCities = ["Bengaluru City", "Belagavi City", "Hubballi–Dharwad City", "Mangaluru City", "Mysuru City", "Kalaburagi City"];

const newWb = xlsx.utils.book_new();
const sheetsData = { "MASTER_MERGED": [] };

data.forEach(row => {
    let dist = row.District || "";
    const station = row.Station || "";
    const name = row.Name || "";
    const unit = row.Unit || "";
    const searchText = `${name} ${station}`;

    // Fix District using keywords
    if (!dist || dist === "State Level" || dist === "Bengaluru City") {
        for (const [d, regex] of Object.entries(distKeywords)) {
            if (regex.test(searchText)) {
                dist = d;
                row.District = d; // Update the row data
                break;
            }
        }
    }

    let target = "General";
    if (specialCities.includes(dist)) target = dist;
    else if (rangeMap[dist]) target = rangeMap[dist];
    else if (unit) target = unit;

    target = target.substring(0, 31).replace(/[\\\/\?\*\[\]\:]/g, "");
    if (!sheetsData[target]) sheetsData[target] = [];
    
    sheetsData[target].push(row);
    sheetsData["MASTER_MERGED"].push(row);
});

// Append sheets
Object.keys(sheetsData).sort().forEach(name => {
    const ws = xlsx.utils.json_to_sheet(sheetsData[name]);
    xlsx.utils.book_append_sheet(newWb, ws, name);
});

xlsx.writeFile(newWb, masterPath);
console.log(`Rebuild complete with ${Object.keys(sheetsData).length} sheets.`);
