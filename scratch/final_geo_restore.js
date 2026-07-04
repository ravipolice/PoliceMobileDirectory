const xlsx = require('xlsx');
const path = require('path');

const masterPath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wbSource = xlsx.readFile(masterPath);
const data = xlsx.utils.sheet_to_json(wbSource.Sheets['MASTER_MERGED_FINAL']);

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

data.forEach(row => {
    const searchText = `${row.Name} ${row.Station} ${row.Unit} ${row.District}`.toLowerCase();
    
    // Priority: If it already has a good district, keep it. If not, re-map.
    if (!row.District || row.District === "State Level") {
        for (const [d, regex] of Object.entries(distKeywords)) {
            if (regex.test(searchText)) {
                row.District = d;
                break;
            }
        }
    }
});

// Save Updated Master
const newWb = xlsx.utils.book_new();
const wsMaster = xlsx.utils.json_to_sheet(data);
xlsx.utils.book_append_sheet(newWb, wsMaster, "MASTER_MERGED_FINAL");
xlsx.writeFile(newWb, masterPath);

console.log("Geographic re-mapping complete for clean 3697 rows.");
