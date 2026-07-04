const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);
const data = xlsx.utils.sheet_to_json(wbSource.Sheets['MASTER_MERGED']);

const distKeywords = {
    "Bagalkote": /Bagalkot|Bagalkote|Mudhol|Jamkhandi|Hunugund|Guledagudda|Ilkal|Bilagi|Mahalingpur|Banahatti|Terdal|Rabkavi/i,
    "Gadag": /Gadag|Naragund|Ron|Mundargi|Shirhatti|Lakshmeshwar|Gajendragad/i,
    "Vijayapura": /Vijayapura|Bijapur|Indi|Muddebihal|Sindagi|Basavana Bagewadi|Babaleshwar|Tikota|Chadchan/i,
    "Dharwad": /Dharwad|Navalgund|Kalghatgi|Alnavar|Kundgol|Annigeri/i,
    "Haveri": /Haveri|Ranebennur|Byadgi|Hanagal|Hirekerur|Shiggaon|Savanur/i,
    "Belagavi": /Belagavi|Belgaum|Chikkodi|Gokak|Athani|Bailhongal|Saundatti|Ramdurg|Hukkeri|Khanapur|Mudalagi|Nippani/i,
    "Ballari": /Ballari|Bellary|Siruguppa|Kampli|Sandur|Kurugodu/i,
    "Vijayanagara": /Vijayanagara|Hospet|Hadagali|Hagaribommanahalli|Kottur|Harapanahalli/i,
    "Raichur": /Raichur|Manvi|Sindhanur|Devadurga|Lingsugur|Maski|Sirwar/i,
    "Koppal": /Koppal|Gangavathi|Kushtagi|Yalaburga|Kanakagiri|Karatagi/i,
    "Bidar": /Bidar|Aurad|Basavakalyan|Bhalki|Humnabad|Chitguppa/i,
    "Kalaburagi": /Kalaburagi|Gulbarga|Aland|Afzalpur|Chincholi|Sedam|Chittapur|Jevargi|Kamalapur|Shahabad/i,
    "Yadgir": /Yadgir|Shorapur|Shahapur|Gurmitkal|Wadagera|Hunasagi/i,
    "Shivamogga": /Shivamogga|Shimoga|Sagara|Shikaripura|Soraba|Thirthahalli|Bhadravathi|Hosanagara/i,
    "Chikkamagaluru": /Chikkamagaluru|Chikmagalur|Kadur|Koppa|Mudigere|Tarikere|Sringeri|Narasimharajapura/i,
    "Udupi": /Udupi|Karkala|Kundapura|Byndoor|Brahmavara|Kaup/i,
    "Uttara Kannada": /Uttara Kannada|Karwar|Sirsi|Kumta|Bhatkal|Haliyal|Ankola|Karwar|Joida|Yellapur/i,
    "Dakshina Kannada": /Dakshina Kannada|Mangaluru|Puttur|Belthangady|Sullia|Bantwal|Kadaba/i,
    "Tumakuru": /Tumakuru|Tumkur|Tiptur|Sira|Gubbi|Madhugiri|Pavagada|Kunigal|Koratagere|Chikkanayakanahalli/i,
    "Chikkaballapura": /Chikkaballapura|Chikballapur|Sidlaghatta|Chintamani|Gauribidanur|Bagepalli/i,
    "Kolar": /Kolar|KGF|Mulbagal|Bangarapet|Malur|Srinivaspura/i,
    "Ramanagara": /Ramanagara|Channapatna|Kanakapura|Magadi|Bidadi/i,
    "Mandya": /Mandya|Maddur|Malavalli|Srirangapatna|Pandavapura|Nagamangala|Krishnarajapet|KR Pet/i,
    "Hassan": /Hassan|Arasikere|Channarayapatna|Holenarasipura|Sakaleshapura|Alur|Arkalgud|Belur/i,
    "Chamarajanagara": /Chamarajanagara|Chamarajanagar|Kollegala|Gundlupet|Yelandur|Hanur/i,
    "Kodagu": /Kodagu|Madikeri|Virajpet|Somwarpet/i,
    "Mysuru": /Mysuru|Mysore|Hunsur|Nanjanagudu|T Narasipura|Periyapatna|HD Kote|Sargur/i,
    "Davanagere": /Davanagere|Davangere|Harihara|Honnali|Jagalur|Channagiri/i,
    "Chitradurga": /Chitradurga|Hiriyur|Hosadurga|Holalkere|Molakalmuru|Challakere/i,
    "Bengaluru City": /Bengaluru City|Bangalore City/i,
    "Bengaluru Rural": /Bengaluru Rural|Bangalore Rural|Doddaballapur|Hosakote|Devanahalli|Nelamangala/i
};

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
    "Ramanagara": "Central Range – Bengaluru", "Tumakuru": "Central Range – Bengaluru", "Bengaluru Rural": "Central Range – Bengaluru",
    "Chitradurga": "Eastern Range – Davanagere", "Davanagere": "Eastern Range – Davanagere", 
    "Haveri": "Eastern Range – Davanagere",
    "Dakshina Kannada": "Western Range – Mangaluru", "Mangaluru": "Western Range – Mangaluru",
    "Udupi": "Western Range – Mangaluru", "Chikkamagaluru": "Western Range – Mangaluru", 
    "Shivamogga": "Western Range – Mangaluru", "Uttara Kannada": "Western Range – Mangaluru"
};

const specialCities = ["Bengaluru City", "Belagavi City", "Hubballi–Dharwad City", "Mangaluru City", "Mysuru City", "Kalaburagi City"];

const newWb = xlsx.utils.book_new();
const sheetsData = { "MASTER_MERGED": [] };

data.forEach(row => {
    const station = row.Station || "";
    const name = row.Name || "";
    const unit = row.Unit || "";
    const searchText = `${name} ${station} ${row.District || ""}`;

    // FORCE Re-map District
    let dist = row.District;
    for (const [d, regex] of Object.entries(distKeywords)) {
        if (regex.test(searchText)) {
            dist = d;
            break;
        }
    }
    row.District = dist;

    let target = "General";
    if (specialCities.includes(dist)) target = dist;
    else if (rangeMap[dist]) target = rangeMap[dist];
    else if (unit) target = unit;

    target = target.substring(0, 31).replace(/[\\\/\?\*\[\]\:]/g, "");
    if (!sheetsData[target]) sheetsData[target] = [];
    
    sheetsData[target].push(row);
    sheetsData["MASTER_MERGED"].push(row);
});

Object.keys(sheetsData).sort().forEach(name => {
    const ws = xlsx.utils.json_to_sheet(sheetsData[name]);
    xlsx.utils.book_append_sheet(newWb, ws, name);
});

xlsx.writeFile(newWb, masterPath);
console.log(`Rebuild complete with ${Object.keys(sheetsData).length} sheets.`);
