const xlsx = require('xlsx');
const ExcelJS = require('exceljs');
const path = require('path');
const fs = require('fs');

// Sources
const csvPath = path.join(__dirname, '..', 'KSP_Contacts_Master_Clean.csv');
const v3Path = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');

// 1. RULE: Rank Mappings (Sorted by length to prioritize ADGP over DGP)
const rankMapping = {
    'DIRECTOR GENERAL & INSPECTOR GENERAL OF POLICE': 'DG & IGP',
    'DIRECTOR GENERAL AND INSPECTOR GENERAL OF POLICE': 'DG & IGP',
    'DG & IGP': 'DG & IGP', 'DIRECTOR GENERAL & IGP': 'DG & IGP', 'DIRECTOR GENERAL AND IGP': 'DG & IGP',
    'ADDITIONAL DIRECTOR GENERAL OF POLICE': 'ADGP', 'ADDITIONAL DGP': 'ADGP', 'ADDL DGP': 'ADGP', 'ADGP': 'ADGP',
    'DGP': 'DGP', 'DIRECTOR GENERAL OF POLICE': 'DGP',
    'INSPECTOR GENERAL OF POLICE': 'IGP', 'IGP': 'IGP',
    'DEPUTY INSPECTOR GENERAL OF POLICE': 'DIG', 'DIGP': 'DIG', 'DIG': 'DIG',
    'ADDITIONAL SUPERINTENDENT OF POLICE': 'Addl.SP', 'ADDL. SP': 'Addl.SP', 'ADDL.SP': 'Addl.SP', 'ADDL SP': 'Addl.SP',
    'SUPERINTENDENT OF POLICE': 'SP', 'SP': 'SP',
    'DEPUTY COMMISSIONER OF POLICE': 'DCP', 'DCP': 'DCP',
    'DEPUTY SUPERINTENDENT OF POLICE': 'DySP', 'DYSP': 'DySP', 'DSP': 'DySP',
    'ASSISTANT SUPERINTENDENT OF POLICE': 'ASP', 'ASP': 'ASP',
    'ASSISTANT COMMISSIONER OF POLICE': 'ACP', 'ACP': 'ACP',
    'COMMANDANT': 'CMDT', 'CMDT': 'CMDT',
    'DEPUTY COMMANDANT': 'DEPT.CMDT', 'DEPT.CMDT': 'DEPT.CMDT',
    'ASSISTANT COMMANDANT': 'ASST.CMDT', 'ASST.CMDT': 'ASST.CMDT',
    'CIRCLE POLICE INSPECTOR': 'CPI', 'CIRCLE PI': 'CPI', 'CPI': 'CPI',
    'RESERVE POLICE INSPECTOR': 'RPI', 'RESERVE PI': 'RPI', 'RPI': 'RPI',
    'WOMEN POLICE INSPECTOR': 'WPI', 'WPI': 'WPI',
    'POLICE INSPECTOR': 'PI', 'PI': 'PI',
    'POLICE SUB INSPECTOR': 'PSI', 'SUB-INSPECTOR': 'PSI', 'PSI': 'PSI',
    'ASSISTANT SUB INSPECTOR': 'ASI', 'ASI': 'ASI',
    'HEAD CONSTABLE': 'HC', 'HC': 'HC',
    'POLICE CONSTABLE': 'PC', 'PC': 'PC',
    'ADDL. IG': 'ADDL.IG', 'ADDITIONAL INSPECTOR GENERAL': 'ADDL.IG',
    'CHIEF SUPERINTENDENT': 'C.SUPT',
    'ASST. SUPERINTENDENT': 'A.SUPT',
    'ASSISTANT SUPERINTENDENT': 'A.SUPT',
    'SUPERINTENDENT OF PRISON': 'SUPT',
    'PRINCIPAL': 'PRIN',
    'AO': 'AO', 'ADMINISTRATIVE OFFICER': 'AO',
    'AAO': 'AAO', 'ASSISTANT ADMINISTRATIVE OFFICER': 'AAO',
    'DDP': 'DDP', 'DEPUTY DIRECTOR': 'DDP',
    'SS': 'SS', 'SECTION SUPERINTENDENT': 'SS',
    'SYSTEM ADMIN': 'System Admin', 'SYSTEM ADMINISTRATOR': 'System Admin',
 'S.S': 'SS',
    'FDA': 'FDA', 'SDA': 'SDA', 'STENO': 'STENO', 'TYPIST': 'TYPIST'
};

const unitMapping = {
    'Law & Order': 'L&O',
    'Criminal Investigation Department': 'CID', 'CID': 'CID',
    'State Intelligence': 'Intelligence', 'Intelligence': 'Intelligence', 'INT.': 'Intelligence',
    'Karnataka State Reserve Police': 'KSRP', 'KSRP': 'KSRP',
    'Internal Security Division': 'ISD', 'ISD': 'ISD',
    'Directorate of Civil Rights Enforcement': 'DCRE', 'DCRE': 'DCRE',
    'Directorate of Forensic Science Laboratory': 'FSL', 'FSL': 'FSL',
    'Finger Print Bureau': 'FPB', 'FPB': 'FPB',
    'Police Training': 'Training', 'Training': 'Training',
    'Karnataka Railways': 'Railway', 'Railway': 'Railway',
    'Coastal Security': 'CSP', 'CSP': 'CSP',
    'State Police Headquarters': 'Admin', 'Headquarters': 'Admin', 'Administration': 'Admin', 'Admin': 'Admin',
    'Karnataka State Police': 'Admin', 'DGP Office': 'Admin',
    'Karnataka Lokayukta': 'Lokayukta', 'Lokayukta': 'Lokayukta',
    'Police Computer Wing': 'Computer', 'Computer': 'Computer',
    'Control Room': 'C/Room', 'C/Room': 'C/Room',
    'Communication, Logistics': 'Wireless', 'Wireless': 'Wireless', 'Motor Transport': 'Wireless',
    'Prison': 'Prison', 'Forest Cell': 'Forest', 'Excise': 'Excise', 'Health': 'Health',
    'VVIP': 'VVIP', 'Traffic': 'Traffic', 'SCRB': 'SCRB',
    'BMTF': 'BMTF', 'Recruitment': 'Recruitment', 'IRB': 'IRB', 'KSPH': 'KSPH', 'KPA': 'Training', 'SPORTI': 'Training',
    'Sports': 'Sports', 'SIT': 'SIT', 'STF': 'STF', 'SAF': 'SAF',
    'Range': 'L&O', 'Commissionerate': 'L&O', 'City': 'L&O', 'Hubballi': 'L&O',
    'Home': 'Home', 'Deputation': 'Others', 'Retired': 'Retired',
    'CAR': 'CAR', 'DAR': 'DAR', 'CCB': 'CCB', 'CCRB': 'CCRB', 'DCRB': 'DCRB', 'CEN': 'CEN', 'Cyber': 'CEN'
};

const comprehensiveRankOrder = [
    "RETD. DG & IGP", "RETD. ADGP", "RETD. IGP",
    "DG & IGP", "Director General of Police", "DG", "DGP", 
    "Additional Director General of Police", "ADGP", 
    "Inspector General of Police", "IGP", 
    "Deputy Inspector General of Police", "DIG", "DIGP",
    "Commandant", "CMDT", "DCP", "Superintendent of Police", "SP", 
    "DEPT.CMDT", "ADDL_SP", "Addl. SP", "Addl SP", "Addl.SP",
    "ASST.CMDT", "DSP", "DySP", "ACP",
    "CPI", "WPI", "PIW", "S.RPI", "RPI", "PI",
    "WPSI", "PSIW", "S.RSI", "RSI", "PSI",
    "S.ARSI", "ASIW", "ARSI", "WASI", "ASI",
    "HCW", "WHC", "S.RHC", "AHC", "CHC", "HC",
    "PCW", "WPC", "S.RPC", "APC", "CPC", "PC",
    "AO", "DD", "AD", "AAO", "CIO", "SIA", "IO", "AIO", "IA",
    "FDA", "SDA", "SS", "STENO", "TYPIST", "PA", "FOLLOWER", "System Admin"
];

const stdMapping = {
    "Bengaluru City": "080", "Bengaluru Urban": "080", "Bengaluru Dist": "080", "Bengaluru Rural": "080", "Ramanagara": "080",
    "Mysuru": "0821", "Chamarajanagar": "08226", "Mandya": "08232", "Hassan": "08172", "Kodagu": "08272",
    "Hubballi": "0836", "Dharwad": "0836", "Gadag": "08372", "Haveri": "08375",
    "Mangaluru": "0824", "Dakshina Kannada": "0824", "Udupi": "0820", "Chikkamagaluru": "08262", "Shivamogga": "08182", "Karwar": "08382", "Uttara Kannada": "08382",
    "Belagavi": "0831", "Bagalkote": "08354", "Vijayapura": "08352",
    "Kalaburagi": "08472", "Bidar": "08482", "Yadgir": "08473",
    "Ballari": "08392", "Vijayanagara": "08394", "Raichur": "08532", "Koppal": "08539",
    "Davanagere": "08192", "Chitradurga": "08194", "Tumakuru": "0816", "Kolar": "08152", "Chikkaballapura": "08156"
};

const cities = ["Bengaluru City", "Belagavi City", "Hubballi Dharwad City", "Kalaburagi City", "Mangaluru City", "Mysuru City"];

// District Alias Mapping for robust search
const districtAliases = {
    "Bellary": "Ballari", "Bellari": "Ballari",
    "Belgaum": "Belagavi", "Bela": "Belagavi",
    "Gulbarga": "Kalaburagi", "Kalburgi": "Kalaburagi",
    "Bangalore": "Bengaluru City", "Benga": "Bengaluru City",
    "Mysore": "Mysuru City", "MYS": "Mysuru City",
    "Mangalore": "Mangaluru City", "MNG": "Mangaluru City",
    "Hubli": "Hubballi Dharwad City", "Dharwad": "Hubballi Dharwad City",
    "Shimoga": "Shivamogga", "Bijapur": "Vijayapura",
    "Chikmagalur": "Chikkamagaluru", "Tumkur": "Tumakuru",
    "Vijayanagar": "Vijayanagara"
};

const dcreMapping = {
    "CENTRAL RANGE OFFICE": { landline: "080-29910724", mobile: "9480806106", email: "spcrdcre@ksp.gov.in" },
    "BENGALURU (RURAL)": { landline: "080-29910723", mobile: "9480806127", email: "sscrdcre@ksp.gov.in" },
    "KOLAR": { landline: "0815-2221647", mobile: "9480806130", email: "klrpsdcre@ksp.gov.in" },
    "TUMKUR": { landline: "0816-2260824", mobile: "9480806132", email: "tkrpsdcre@ksp.gov.in" },
    "RAMANAGAR": { landline: "080-29911891", mobile: "9480806134", email: "rmnpsdcre@ksp.gov.in" },
    "CHIKKABALLAPURA": { landline: "08156-272733", mobile: "9480806136", email: "cbppsdcre@ksp.gov.in" },
    "BENGALURU CITY RANGE": { landline: "080-29910793", mobile: "9480806105", email: "spbngcitydcre@ksp.gov.in" },
    "BENGALURU CITY EAST": { landline: "080-29910753", mobile: "9480806121", email: "bngeastpsdcre@ksp.gov.in" },
    "BENGALURU CITY WEST": { landline: "080-29910763", mobile: "9480806124", email: "bngwestpsdcre@ksp.gov.in" },
    "EASTERN RANGE OFFICE": { landline: "08192-259574", mobile: "9480806109", email: "sperdcre@ksp.gov.in" },
    "DAVANAGERE": { landline: "08192-259575", mobile: "9480806122", email: "dvgpsdcre@ksp.gov.in" },
    "SHIVAMOGGA": { landline: "08182-200788", mobile: "9480806158", email: "shipsdcre@ksp.gov.in" },
    "HAVERI": { landline: "08375-200867", mobile: "9480806164", email: "hvrpsdcre@ksp.gov.in" },
    "CHITRADURGA": { landline: "08194-200719", mobile: "9480806160", email: "ctapsdcre@ksp.gov.in" },
    "WESTERN RANGE OFFICE": { landline: "0824-2453644", mobile: "9480806110", email: "spwrdcre@ksp.gov.in" },
    "DAKSHINA KANNADA": { landline: "0824-2003544", mobile: "9480806166", email: "dkpsdcre@ksp.gov.in" },
    "UTTARA KANNADA": { landline: "08382-221214", mobile: "9480806168", email: "ukpsdcre@ksp.gov.in" },
    "UDUPI": { landline: "0820-2003644", mobile: "9480806170", email: "udppsdcre@ksp.gov.in" },
    "CHIKMAGALUR": { landline: "08262-232335", mobile: "9480806172", email: "ckmpsdcre@ksp.gov.in" },
    "SOUTHERN RANGE OFFICE": { landline: "0821-2344912", mobile: "9480806107", email: "spsrdcre@ksp.gov.in" },
    "MYSURU": { landline: "0821-2344970", mobile: "9480806138", email: "mycpsdcre@ksp.gov.in" },
    "MANDYA": { landline: "08232-230703", mobile: "9480806142", email: "mdypsdcre@ksp.gov.in" },
    "CHAMARAJANAGAR": { landline: "08226-224122", mobile: "9480806144", email: "chnpsdcre@ksp.gov.in" },
    "HASSAN": { landline: "08172-200023", mobile: "9480806140", email: "hsnpsdcre@ksp.gov.in" },
    "KODAGU": { landline: "0827-2221050", mobile: "9480806146", email: "mcrpsdcre@ksp.gov.in" },
    "NORTHERN RANGE OFFICE": { landline: "0831-2405215", mobile: "9480806108", email: "spnrdcre@ksp.gov.in" },
    "BELAGAVI": { landline: "0831-2453802", mobile: "9480806148", email: "bgmpsdcre@ksp.gov.in" },
    "VIJAYAPURA": { landline: "08352-200150", mobile: "9480806156", email: "vjppsdcre@ksp.gov.in" },
    "BAGALKOT": { landline: "08354-295768", mobile: "9480806150", email: "bgkpsdcre@ksp.gov.in" },
    "DHARWAD": { landline: "0836-2445580", mobile: "9480806152", email: "dwdpsdcre@ksp.gov.in" },
    "GADAG": { landline: "08372-200223", mobile: "9480806154", email: "gdgpsdcre@ksp.gov.in" },
    "NORTHEASTERN RANGE OFFICE": { landline: "08472-263647", mobile: "9480806111", email: "spnerdcre@ksp.gov.in" },
    "KALABURAGI": { landline: "08472-255266", mobile: "9480806174", email: "kbcpsdcre@ksp.gov.in" },
    "YADGIR": { landline: "08473-200141", mobile: "9480806176", email: "ydrpsdcre@ksp.gov.in" },
    "BIDAR": { landline: "08482-200988", mobile: "9480806178", email: "bdrpsdcre@ksp.gov.in" },
    "BALLARI RANGE OFFICE": { landline: "08392-234491", mobile: "9480806112", email: "spblrdcre@ksp.gov.in" },
    "BALLARI": { landline: "08392-299038", mobile: "9480806180", email: "blrpsdcre@ksp.gov.in" },
    "RAICHUR": { landline: "08532-230100", mobile: "9480806182", email: "rcrpsdcre@ksp.gov.in" },
    "KOPPAL": { landline: "08539-230227", mobile: "9480806184", email: "kplpsdcre@ksp.gov.in" },
    "VIJAYANAGARA": { landline: "08394-266100", mobile: "9480806186", email: "vjnpsdcre@ksp.gov.in" },
    "DCRE HQ": { landline: "080-22268001", mobile: "9480806114", email: "hqpsdcre@ksp.gov.in" }
};

const dcreHqMapping = {
    "DGP": { landline: "080-22268723", mobile: "9480806101", email: "dgpdcre@ksp.gov.in" },
    "DIGP": { landline: "080-22374163", mobile: "9480806102", email: "digpcre@ksp.gov.in" },
    "SP (HQ)": { landline: "080-22266891", mobile: "9480806103", email: "sphqdcre@ksp.gov.in" },
    "SP (ADMIN)": { landline: "080-22268003", mobile: "9480806104", email: "spadmindcre@ksp.gov.in" },
    "A.O": { landline: "080-22260827", mobile: "9480806119", email: "aohqdcre@ksp.gov.in" },
    "DDP": { landline: "", mobile: "9480806113", email: "ddpdcre@ksp.gov.in" },
    "SSO": { landline: "080-22260827", mobile: "", email: "ss1hqdcre@ksp.gov.in" },
    "PI, MTO": { landline: "", mobile: "9480806117", email: "mtohqdcre@ksp.gov.in" },
    "PI, WIRELESS": { landline: "", mobile: "9480806118", email: "" },
    "CONTROL ROOM": { landline: "080-22268001", mobile: "9480806100", email: "Ndcrehqcontrolroom@ksp.gov.in" }
};

function getRangeForDistrict(district) {
    if (!district) return "";
    const d = district.trim();
    if (["Bengaluru Urban", "Bengaluru Dist", "Kolar", "Chikkaballapura", "Ramanagara", "Tumakuru"].includes(d)) return "Central Range";
    if (["Belagavi Dist", "Vijayapura", "Dharwad", "Bagalkote", "Gadag"].includes(d)) return "Northern Range";
    if (["Kalaburagi", "Bidar", "Yadgir"].includes(d)) return "North Eastern Range";
    if (["Ballari", "Raichur", "Koppal", "Vijayanagara"].includes(d)) return "Ballari Range";
    if (["Mysuru Dist", "Chamarajanagar", "Hassan", "Kodagu", "Mandya"].includes(d)) return "Southern Range";
    if (["Dakshina Kannada", "Udupi", "Chikkamagaluru", "Shivamogga", "Uttara Kannada"].includes(d)) return "Western Range";
    if (["Davanagere", "Chitradurga", "Haveri"].includes(d)) return "Davangere Range";
    if (cities.includes(d)) return "Commissionerate";
    return "";
}

// Helpers
function cleanPhone(val) {
    if (!val) return "";
    return String(val).replace(/\s/g, '').replace(/—/g, '').replace(/[â\x80-\x9F]/g, "");
}

function getRank(rawRank) {
    if (!rawRank) return "";
    let r = String(rawRank).toUpperCase().trim();
    let isRetd = r.includes("RETD") || r.includes("RETIRED");

    // 1. Personnel Rank Mapping Check
    const sortedKeys = Object.keys(rankMapping).sort((a, b) => b.length - a.length);
    
    // Priority 1: Strict Prefix
    for (const key of sortedKeys) {
        const escapedKey = key.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
        let pattern = "^";
        pattern += escapedKey;
        if (/[A-Za-z0-9]$/.test(key)) pattern += "\\b";
        
        const regex = new RegExp(pattern, 'i');
        if (regex.test(r)) return (isRetd ? "RETD. " : "") + rankMapping[key];
    }

    // Priority 2: Contains (but skip ministerial PA/Office contexts)
    for (const key of sortedKeys) {
        const escapedKey = key.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
        let pattern = "";
        if (/^[A-Za-z0-9]/.test(key)) pattern += "\\b";
        pattern += escapedKey;
        if (/[A-Za-z0-9]$/.test(key)) pattern += "\\b";
        
        const regex = new RegExp(pattern, 'i');
        const match = r.match(regex);
        if (match) {
            const matchIndex = match.index;
            const contextBefore = r.substring(0, matchIndex);
            if (!contextBefore.includes("TO ") && !contextBefore.includes("OFFICE") && !contextBefore.includes("OF ")) {
                return (isRetd ? "RETD. " : "") + rankMapping[key];
            }
        }
    }

    // Special case for DG & IGP
    if (r.includes("DG") && r.includes("IGP") && !r.includes("ADGP")) {
        const dgIndex = r.indexOf("DG");
        const igpIndex = r.indexOf("IGP");
        if (dgIndex < igpIndex && dgIndex < 10) return (isRetd ? "RETD. " : "") + "DG & IGP";
    }

    // If no official personnel rank is found, leave it blank as requested
    return "";
}

function shortenNames(name) {
    if (!name) return "";
    let n = String(name).trim().replace(/[â\x80-\x9F]/g, "");
    const sortedKeys = Object.keys(rankMapping).sort((a, b) => b.length - a.length);
    for (const key of sortedKeys) {
        const regex = new RegExp(`\\b${key.replace('.', '\\.')}\\b`, 'gi');
        n = n.replace(regex, rankMapping[key]);
    }
    return n;
}

function getUnitShortCode(rawUnit) {
    if (!rawUnit) return "Others";
    let u = String(rawUnit).trim();
    
    // 1. Pre-clean: Remove common rank prefixes often found in unit names
    u = u.replace(/^(ADGP|IGP|DIG|SP|DySP|PI|PSI|PA|AAO|SS|FDA|SDA|STENO|TYPIST)[,\s]+/i, "").trim();
    
    // 2. Explicit check for common Law & Order variants
    if (u.includes("L&O") || u.includes("L & O") || u.includes("Law & Order") || u.includes("Law and Order")) return "L&O";

    // 3. Loop through mapping
    // Sort keys by length (longest first) to catch "Bengaluru City" before "City"
    const sortedKeys = Object.keys(unitMapping).sort((a, b) => b.length - a.length);
    for (const key of sortedKeys) {
        if (u.toUpperCase().includes(key.toUpperCase())) return unitMapping[key];
    }
    
    return u;
}

async function ultimateRebuild() {
    console.log("Starting ULTIMATE REBUILD with UNIT and RANK short codes...");
    const csvContent = fs.readFileSync(csvPath, 'utf8');
    const csvData = xlsx.utils.sheet_to_json(xlsx.read(csvContent, { type: 'string' }).Sheets.Sheet1);
    const masterRecords = [];

    const karnatakaDistricts = [
        "Bengaluru Rural", "Bengaluru Urban", "Chikkaballapura", "Kolar", "Ramanagara", "Tumakuru",
        "Mysuru", "Chamarajanagar", "Mandya", "Hassan", "Kodagu",
        "Dakshina Kannada", "Udupi", "Chikkamagaluru", "Uttara Kannada", "Shivamogga",
        "Davanagere", "Davangere", "Chitradurga", "Haveri",
        "Belagavi", "Belgaum", "Bagalkote", "Bagalkot", "Vijayapura", "Bijapur", "Dharwad", "Gadag",
        "Kalaburagi", "Gulbarga", "Bidar", "Yadgir",
        "Ballari", "Bellary", "Koppal", "Raichur", "Vijayanagara"
    ];
    
    const districtAliases = {
        "Bellary": "Ballari", "Bellari": "Ballari",
        "Belgaum": "Belagavi", "Bela": "Belagavi",
        "Gulbarga": "Kalaburagi", "Kalburgi": "Kalaburagi",
        "Bangalore": "Bengaluru Rural",
        "Mysore": "Mysuru",
        "Mangalore": "Dakshina Kannada",
        "Hubli": "Dharwad", "Dharwad": "Dharwad",
        "Shimoga": "Shivamogga", "Bijapur": "Vijayapura",
        "Chikmagalur": "Chikkamagaluru", "Tumkur": "Tumakuru",
        "Vijayanagar": "Vijayanagara"
    };

    let currentRange = "";
    let currentDistrict = "";
    let currentSection = "";

    const firstRow = csvData[0] || {};
    const sectionKey = Object.keys(firstRow).find(k => k.includes("Section")) || "Section";
    const unitKey = Object.keys(firstRow).find(k => k.includes("Unit")) || "Unit";
    const desKey = Object.keys(firstRow).find(k => k.includes("Designation")) || "Designation";
    const phoneKey = Object.keys(firstRow).find(k => k.includes("Phone")) || "Phone";
    const emailKey = Object.keys(firstRow).find(k => k.includes("Email")) || "Email";

    csvData.forEach(r => {
        const sectionRaw = String(r[sectionKey] || "").trim();
        if (sectionRaw) {
            currentSection = sectionRaw;
        }
        const rawCol1 = currentSection;
        const rawCol2 = String(r[unitKey] || "").trim();
        const name = shortenNames(r[desKey] || "");
        const rank = getRank(r[desKey] || "");
        
        let unit = getUnitShortCode(rawCol2 || rawCol1);
        let range = "";
        let district = "";
        let section = rawCol1;
        let landlines = [];
        let mobiles = [];
        let email = "";

        // Keep track of current district in Ranges section
        if (rawCol1 === "Ranges" || rawCol1.includes("Range")) {
            if (rawCol2 !== currentRange) {
                currentRange = rawCol2;
                currentDistrict = ""; // Reset district when range changes
            }
            
            const des = String(r[desKey] || "").trim();
            const desUpper = des.toUpperCase();
            
            for (const dist of karnatakaDistricts) {
                const distUpper = dist.toUpperCase();
                if (desUpper.includes(`SP, ${distUpper}`) || 
                    desUpper.includes(`SP ${distUpper}`) || 
                    desUpper.includes(`SUPERINTENDENT OF POLICE, ${distUpper}`) ||
                    desUpper.includes(`SUPERINTENDENT OF POLICE ${distUpper}`) ||
                    (desUpper.includes("ADDL. SP") && desUpper.includes(distUpper)) ||
                    (desUpper.includes("ADDL.SP") && desUpper.includes(distUpper)) ||
                    (desUpper.includes("ADDL SP") && desUpper.includes(distUpper)) ||
                    (desUpper.includes("DSP") && desUpper.includes(distUpper) && !desUpper.includes("LOKAYUKTA") && !desUpper.includes("KLA"))
                ) {
                    currentDistrict = districtAliases[dist] || dist;
                    break;
                }
            }
        }

        // 1. CID Rule: Always Bengaluru City
        if (unit === "CID") {
            district = "Bengaluru City";
            range = "";
        }
        // 2. L&O Rule: Commissionerates or Ranges
        else if (rawCol1 === "Commissionerates" || cities.includes(rawCol2)) {
            unit = "L&O";
            range = rawCol2;
            district = rawCol2;
        } 
        else if (rawCol1.includes("Range")) {
            unit = "L&O";
            range = rawCol1;
            
            let rowDistrict = "";
            const des = String(r[desKey] || "").trim();
            
            // Check for direct district name in Designation (e.g. "AAO Udupi", "AAO Dharwad")
            for (const dist of karnatakaDistricts) {
                if (new RegExp(`\\b${dist}\\b`, 'i').test(des)) {
                    rowDistrict = districtAliases[dist] || dist;
                    break;
                }
            }
            
            if (rowDistrict) {
                district = rowDistrict;
            } else if (currentDistrict) {
                district = currentDistrict;
            } else {
                district = rawCol2;
            }
        }
        // 3. Regional Scope Units: Look for district name in the fields
        else if (["Intelligence", "KSRP", "ISD", "DCRE", "Railway"].includes(unit)) {
            const searchString = (rawCol1 + " " + rawCol2 + " " + (r.Designation || "")).toLowerCase();
            
            // Priority 1: Check Aliases first
            let foundDistrict = null;
            for (const [alias, realName] of Object.entries(districtAliases)) {
                if (searchString.includes(alias.toLowerCase())) {
                    foundDistrict = realName;
                    break;
                }
            }

            // Priority 2: Check official names (longest first)
            if (!foundDistrict) {
                const officialDistricts = Object.keys(stdMapping).sort((a, b) => b.length - a.length);
                foundDistrict = officialDistricts.find(d => searchString.includes(d.toLowerCase()));
            }

            if (foundDistrict) {
                district = foundDistrict;
                range = getRangeForDistrict(foundDistrict);
            } else {
                // If no specific district found, it's HQ staff
                district = "Bengaluru City";
                range = "";
            }

            // DCRE Specific Contact Logic
            if (unit === "DCRE") {
                const dcreSearch = (rawCol1 + " " + (r.Designation || "")).toUpperCase();
                
                // Check HQ mapping first
                for (const [hqKey, info] of Object.entries(dcreHqMapping)) {
                    if (dcreSearch.includes(hqKey)) {
                        if (info.landline) landlines.push(info.landline);
                        if (info.mobile) mobiles.push(info.mobile);
                        if (info.email) email = info.email;
                        break;
                    }
                }

                // Check Regional mapping
                for (const [regKey, info] of Object.entries(dcreMapping)) {
                    if (dcreSearch.includes(regKey)) {
                        if (info.landline && !landlines.includes(info.landline)) landlines.push(info.landline);
                        if (info.mobile && !mobiles.includes(info.mobile)) mobiles.push(info.mobile);
                        if (info.email) email = info.email;
                        break;
                    }
                }
            }
        }
        // 4. Default: Admin / Others
        else {
            district = (unit === "Admin") ? "Bengaluru City" : "";
            range = "";
        }

        // 5. Functional Override & Section Extraction
        const approvedUnits = [
            "Admin", "BMTF", "CID", "Computer", "C/Room", "DCRE", "FPB", "FSL", "Forest", "Home",
            "IRB", "ISD", "Intelligence", "KSPH", "KSRP", "L&O", "Lokayukta", "Others", "Prison",
            "Railway", "Recruitment", "Retired", "SAF", "SIT", "STF", "Sports", "Training", "Wireless",
            "CAR", "DAR", "CCB", "CCRB", "DCRB", "CEN", "Traffic", "VVIP"
        ];

        if (unit === "Admin" || unit === "Others" || unit === "L&O" || !approvedUnits.includes(unit)) {
            const combinedForExtraction = `${rawCol1} ${rawCol2} ${r.Designation || ""}`.toUpperCase();
            
            let specializedMatch = null;
            const specializedOverrideUnits = ["CAR", "DAR", "CCB", "CCRB", "DCRB", "CEN", "TRAFFIC", "VVIP", "CSB", "FSL", "FPB", "WIRELESS", "CONTROL ROOM"];
            
            for (const sp of specializedOverrideUnits) {
                if (new RegExp(`\\b${sp}\\b`).test(combinedForExtraction)) {
                    specializedMatch = sp === "TRAFFIC" ? "Traffic" : 
                                       sp === "WIRELESS" ? "Wireless" : 
                                       sp === "CONTROL ROOM" ? "C/Room" : sp;
                    break;
                }
            }
            
            if (specializedMatch) {
                unit = specializedMatch;
                if (unit === "Admin") district = "Bengaluru City";
            } else if (unit !== "L&O") {
                const functionalUnit = getUnitShortCode(r.Designation || "");
                if (approvedUnits.includes(functionalUnit)) {
                    // It's a major functional unit (KSRP, Railway, etc.)
                    unit = functionalUnit;
                    district = (unit === "Admin") ? "Bengaluru City" : district;
                } else {
                    // It's a sub-section (LAW, HRM, PRO, etc.)
                    if (functionalUnit && functionalUnit !== "Admin" && functionalUnit !== "Others") {
                        section = functionalUnit;
                    }
                    unit = "Admin"; // Revert to Admin for sub-sections
                    district = "Bengaluru City";
                }
            }
        }

        // 6. FINAL RULE: If retired, force UNIT to "Retired"
        if (rank.includes("RETD. ")) {
            unit = "Retired";
        }

        const rawPhones = String(r[phoneKey] || "").split(/[,\/]/);
        
        rawPhones.forEach(p => {
            const cp = cleanPhone(p);
            if (cp.match(/^[789]\d{9}$/) || cp.length === 10) {
                if (!mobiles.includes(cp)) mobiles.push(cp);
            }
            else if (cp.length >= 6 && cp.length <= 8) {
                if (!landlines.includes(cp)) landlines.push(cp);
            }
            else if (cp.startsWith('0') && cp.includes('-')) {
                let parts = cp.split('-');
                if (parts[1] && parts[1].length >= 6) {
                    if (!landlines.includes(parts[1])) landlines.push(parts[1]);
                }
            }
        });

        const std = stdMapping[district] || "080";
        const rowEmails = String(r[emailKey] || "").match(/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/g) || [];
        let finalEmails = email ? [email] : [];
        rowEmails.forEach(e => {
            if (!finalEmails.includes(e)) finalEmails.push(e);
        });

        masterRecords.push({
            agid: "", 
            UNIT: unit,
            Range: range,
            District: district,
            Section: section,
            Name: name,
            Rank: rank,
            station: r.Section || "",
            office1: landlines[0] ? (landlines[0].startsWith("0") ? landlines[0] : `${std}-${landlines[0]}`) : "",
            "office 2": landlines[1] ? (landlines[1].startsWith("0") ? landlines[1] : `${std}-${landlines[1]}`) : "",
            "mobile 1": mobiles[0] || "",
            "mobile 2": mobiles[1] || "",
            email1: finalEmails[0] || "",
            email2: finalEmails[1] || ""
        });
    });

    function getRankScore(rankStr) {
        if (!rankStr) return 9999;
        const rankUpper = String(rankStr).toUpperCase();
        for (let i = 0; i < comprehensiveRankOrder.length; i++) {
            if (rankUpper.includes(comprehensiveRankOrder[i].toUpperCase())) {
                return i;
            }
        }
        return 9999;
    }

    masterRecords.sort((a, b) => {
        const scoreA = getRankScore(a.Rank);
        const scoreB = getRankScore(b.Rank);
        if (scoreA !== scoreB) {
            return scoreA - scoreB;
        }
        // Secondary sort by District
        if (a.District !== b.District) {
            return (a.District || "").localeCompare(b.District || "");
        }
        // Tertiary sort by Name
        return (a.Name || "").localeCompare(b.Name || "");
    });

    const workbook = new ExcelJS.Workbook();
    const headers = ["agid", "UNIT", "Range", "District", "Section", "Name", "Rank", "station", "office1", "office 2", "mobile 1", "mobile 2", "email1", "email2"];

    const wsMaster = workbook.addWorksheet("MASTER_MERGED_FINAL");
    wsMaster.columns = headers.map(h => ({ header: h, key: h, width: 20 }));
    masterRecords.forEach(r => wsMaster.addRow(r));
    wsMaster.getRow(1).font = { bold: true };
    wsMaster.views = [{ state: 'frozen', ySplit: 1 }];

    const uniqueUnits = [...new Set(masterRecords.map(r => r.UNIT))];
    uniqueUnits.forEach(u => {
        const sheetName = String(u).substring(0, 31).replace(/[\\\/\?\*\[\]]/g, "");
        const ws = workbook.addWorksheet(sheetName);
        ws.columns = headers.map(h => ({ header: h, key: h, width: 20 }));
        masterRecords.filter(r => r.UNIT === u).forEach(r => ws.addRow(r));
        ws.getRow(1).font = { bold: true };
        ws.views = [{ state: 'frozen', ySplit: 1 }];
    });

    await workbook.xlsx.writeFile(v3Path);
    console.log(`ULTIMATE REBUILD complete. ${masterRecords.length} rows processed.`);
}

ultimateRebuild().catch(console.error);
