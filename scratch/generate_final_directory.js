const fs = require('fs');
const path = require('path');
const { parse } = require('csv-parse/sync');
const xlsx = require('xlsx');
const ExcelJS = require('exceljs');

// 1. Configuration
const INPUT_CSV = path.join(__dirname, '../KSP_Contacts_Master_Clean.csv');
const OUTPUT_EXCEL = path.join(__dirname, '../KSP_Contacts_Final_Directory_V3.xlsx');
const OUTPUT_APP_CSV = path.join(__dirname, '../KSP_Officers_App.csv');

// 2. Load Data (Will be done inside async block)

// 3. Helper Maps & Logic
const rankShortMap = {
    'Director General & Inspector General of Police': 'DG & IGP',
    'Additional Director General': 'ADGP',
    'Deputy Inspector General': 'DIG',
    'Commissioner of Police': 'CP',
    'Deputy Commissioner of Police': 'DCP',
    'Assistant Commissioner of Police': 'ACP',
    'Police Inspector': 'PI',
    'Police Sub-Inspector': 'PSI',
    'Assistant Superintendent of Police': 'ASP',
    'Superintendent of Police': 'SP',
    'Deputy Superintendent of Police': 'DSP',
    'DSP': 'DSP',
    'Additional Superintendent of Police': 'ADDL_SP',
    'Circle Police Inspector': 'CPI',
    'Assistant Police Inspector': 'API',
    'Assistant Sub-Inspector': 'ASI',
    'Head Constable': 'HC',
    'Police Constable': 'PC',
    'P.A. to': 'PA',
    'Ministerial Staff': 'Min. Staff',
    'SP (W)': 'SP',
    'SP(W)': 'SP',
    'DySP(W)': 'DSP',
    'PI(W)': 'PI',
    'Assistant Commandant': 'ASST.CMDT',
    'Deputy Commandant': 'DEPT.CMDT',
    'Commandant': 'CMDT',
    'DEPUTY COMMANDANT KSISF': 'DEPT.CMDT',
    'PA TO': 'PA',
    'PATO': 'PA',
    'Superintendent': 'SS',
    'Assistant Director': 'AD',
    'Deputy Director': 'DD',
    'Steno': 'STENO',
    'Typist': 'TYPIST',
    'Follower': 'FOLLOWER',
    'Inspector of Accounts': 'IA',
    'Assistant Intelligence Officer': 'AIO',
    'Intelligence Officer': 'IO',
    'Special Intelligence Officer': 'SIA',
    'Chief Intelligence Officer': 'CIO'
};

const rankWeightMap = {
    'DGP': 1, 'ADGP': 2, 'IGP': 3, 'DIGP': 4, 'DIG': 5, 'CP': 6, 'ADDL.CP': 7, 'JT.CP': 8, 'SP': 9, 'DCP': 10, 'ADDL.SP': 11, 'ASP': 12, 'DSP': 13, 'ACP': 14,
    'PI': 15, 'PSI': 16, 'ASI': 17, 'HC': 18, 'PC': 19, 'PA': 20, 'FDA': 21, 'SDA': 22, 'AAO': 23, 'Supdt': 24
};

function getRankWeight(rank) {
    if (!rank) return 99;
    for (const [r, w] of Object.entries(rankWeightMap)) {
        if (rank.toUpperCase().startsWith(r)) return w;
    }
    return 99;
}

const hqKeywordMap = {
    'KSRP': 'KSRP',
    'Crime': 'Crime',
    'CTRS': 'C&TS',
    'Communication': 'C&TS',
    'ANTF': 'ANTF',
    'Railways': 'Railways',
    'L&O': 'Law & Order',
    'Intelligence': 'Intelligence',
    'KSISF': 'KSISF',
    'Grievances': 'G&HR',
    'Law': 'Law & Order',
    'General': 'General',
    'GHR': 'G&HR',
    'GHA': 'G&HR',
    'Human Rights': 'G&HR',
    'Admin': 'Administration',
    'MTO': 'MTO',
    'PRO': 'PRO',
    'ISD': 'ISD',
    'C/Room': 'C/Room',
    'Wireless': 'Wireless',
    'FSL': 'FSL',
    'Recruitment': 'Recruitment',
    'Training': 'Training',
    'SCRB': 'SCRB',
    'Computer': 'SCRB'
};

const districtMap = {
    'Bengaluru': 'Bengaluru City',
    'Mysuru': 'Mysuru',
    'Mangaluru': 'Mangaluru',
    'Belagavi': 'Belagavi',
    'Kalaburagi': 'Kalaburagi',
    'Hubballi': 'Hubballi-Dharwad City',
    'Dharwad': 'Dharwad',
    'Ballari': 'Ballari',
    'Bagalkote': 'Bagalkote',
    'Bidar': 'Bidar',
    'Chamarajanagara': 'Chamarajanagara',
    'Chikkaballapura': 'Chikkaballapura',
    'Chikkamagaluru': 'Chikkamagaluru',
    'Chitradurga': 'Chitradurga',
    'Dakshina Kannada': 'Dakshina Kannada',
    'Davanagere': 'Davanagere',
    'Gadag': 'Gadag',
    'Hassan': 'Hassan',
    'Haveri': 'Haveri',
    'Kodagu': 'Kodagu',
    'Kolar': 'Kolar',
    'Koppal': 'Koppal',
    'Mandya': 'Mandya',
    'Raichur': 'Raichur',
    'Ramanagara': 'Ramanagara',
    'Shivamogga': 'Shivamogga',
    'Tumakuru': 'Tumakuru',
    'Udupi': 'Udupi',
    'Uttara Kannada': 'Uttara Kannada',
    'Vijayapura': 'Vijayapura',
    'Yadgiri': 'Yadgiri'
};

const unitShortMap = {
    'Criminal Investigation Department (CID)': 'CID',
    'Internal Security Division (ISD)': 'ISD',
    'Karnataka State Reserve Police (KSRP)': 'KSRP',
    'Directorate of Forensic Science Laboratory (FSL)': 'FSL',
    'Police Computer Wing (PCW) / SCRB': 'SCRB',
    'Directorate of Civil Rights Enforcement': 'DCRE',
    'Karnataka Railways Police': 'Railways',
    'Wireless': 'Wireless',
    'Bengaluru Metropolitan Task Force (BMTF)': 'BMTF',
    'Department of Prisons & Correctional Services': 'Prisons',
    'Karnataka Police Academy (KPA)': 'KPA',
    'Karnataka State Sports Promotion Control Board': 'Sports Board',
    'Karnataka Lokayukta': 'Lokayukta',
    'State Intelligence': 'Intelligence',
    'Internal Security Division': 'ISD',
    'Criminal Investigation Department': 'CID',
    'Finger Print Bureau (FPB)': 'FPB',
    'Police Recruitment Wing': 'Recruitment',
    'Police Training Wing': 'Training',
    'Police Training Institutions': 'Training',
    'Communication, Logistics & Modernisation': 'C&TS',
    'Special Investigation Team (SIT)': 'SIT',
    'Special Action Force (SAF) - Mangaluru': 'SAF',
    'Karnataka State Reserve Police': 'KSRP',
    'KSRP Battalions': 'KSRP',
    'IRB Battalions & Training Institutions': 'IRB',
    'State Police Headquarters': 'SPHQ',
    'Control Rooms in Bengaluru': 'C/Room',
    'Retired Police Officers': 'Retired',
    'Home, Civil Defence, Fire & Emergency Services': 'HG & CD',
    'Officers on State Deputation': 'On Deputation'
};

const districtToRangeMap = {
    'Bengaluru City': 'Bengaluru City', 'Mysuru City': 'Mysuru City', 'Hubballi-Dharwad': 'Hubballi-Dharwad City',
    'Mangaluru City': 'Mangaluru City', 'Belagavi City': 'Belagavi City', 'Kalaburagi City': 'Kalaburagi City',
    'Bengaluru Dist': 'Central Range', 'Chikkaballapura': 'Central Range', 'Ramanagara': 'Central Range', 'Tumakuru': 'Central Range', 'Kolar': 'Central Range',
    'Dakshina Kannada': 'Western Range', 'Udupi': 'Western Range', 'Uttara Kannada': 'Western Range', 'Chikkamagaluru': 'Western Range',
    'Davanagere': 'Eastern Range', 'Shivamogga': 'Eastern Range', 'Haveri': 'Eastern Range', 'Chitradurga': 'Eastern Range',
    'Belagavi Dist': 'Belagavi Range', 'Dharwad': 'Northern Range', 'Gadag': 'Northern Range', 'Vijayapura': 'Northern Range', 'Bagalkote': 'Northern Range',
    'Kalaburagi': 'North Eastern Range', 'Bidar': 'North Eastern Range', 'Yadgiri': 'North Eastern Range',
    'Mysuru': 'Southern Range', 'Hassan': 'Southern Range', 'Kodagu': 'Southern Range', 'Chamarajanagara': 'Southern Range', 'Mandya': 'Southern Range',
    'Ballari': 'Ballari Range', 'Vijayanagara': 'Ballari Range', 'Koppal': 'Ballari Range', 'Raichur': 'Ballari Range'
};

function getRangeForDistrict(dist) {
    if (!dist) return '';
    for (const [d, r] of Object.entries(districtToRangeMap)) {
        if (dist.includes(d)) return r;
    }
    return '';
}

const stdCodeMap = {
    'Bengaluru': '080', 'Ramanagara': '080', 'Chikkaballapura': '08156', 'Kolar': '08152', 'Tumakuru': '0816',
    'Mysuru': '0821', 'Mandya': '08232', 'Chamarajanagara': '08226', 'Hassan': '08172', 'Kodagu': '08272',
    'Mangaluru': '0824', 'Dakshina Kannada': '0824', 'Udupi': '0820', 'Chikkamagaluru': '08262', 'Uttara Kannada': '08382',
    'Belagavi': '0831', 'Dharwad': '0836', 'Hubballi': '0836', 'Gadag': '08372', 'Haveri': '08375', 'Bagalkote': '08354', 'Vijayapura': '08352',
    'Kalaburagi': '08472', 'Bidar': '08482', 'Yadgiri': '08473', 'Raichur': '08532', 'Koppal': '08394', 'Ballari': '08392', 'Vijayanagara': '08392',
    'Davanagere': '08192', 'Chitradurga': '08194', 'Shivamogga': '08182'
};

function applySTD(phone, dist) {
    if (!phone || phone.length > 8 || phone.startsWith('0')) return phone;
    let code = '080'; // Default
    for (const [d, c] of Object.entries(stdCodeMap)) {
        if (dist.includes(d)) {
            code = c;
            break;
        }
    }
    return `${code}-${phone}`;
}

const rangeKeywords = ['Central', 'Western', 'Eastern', 'Northern', 'Southern', 'North Eastern', 'Ballari Range', 'Belagavi Range'];
const hqKeys = Object.keys(hqKeywordMap).sort((a, b) => b.length - a.length);

async function generateExcel(data, headers, outputPath) {
    const workbook = new ExcelJS.Workbook();
    const sheet = workbook.addWorksheet('KSP_Directory', {
        views: [{ state: 'frozen', ySplit: 1 }]
    });

    sheet.columns = headers.map(h => ({
        header: h,
        key: h,
        width: 25
    }));

    sheet.addRows(data);

    sheet.autoFilter = {
        from: 'A1',
        to: { row: 1, column: headers.length }
    };

    sheet.getRow(1).font = { bold: true };
    sheet.getRow(1).fill = {
        type: 'pattern',
        pattern: 'solid',
        fgColor: { argb: 'FFEEEEEE' }
    };

    await workbook.xlsx.writeFile(outputPath);
}

// 4. Processing Logic
(async () => {
    console.log('--- STARTING FINAL DIRECTORY GENERATION (V3) ---');
    let inputCsvContent = fs.readFileSync(INPUT_CSV, 'utf8');
    if (inputCsvContent.charCodeAt(0) === 0xFEFF) inputCsvContent = inputCsvContent.slice(1);
    const records = parse(inputCsvContent, { columns: true, skip_empty_lines: true });
    console.log(`Loaded ${records.length} records.`);

    const allOfficers = records.map((row, index) => {
        let rawDesig = (row.Designation || '').replace(/Control Room/gi, 'C/Room');
        let section = (row.Section || '').trim();
        let unit = (row.Unit || '').trim();
        // 🎯 Surgical Unit Shortening (Only for known official long names)
        for (const [long, short] of Object.entries(unitShortMap)) {
            if (unit === long) {
                unit = short;
                break;
            }
        }
        const originalUnit = unit; 
        const originalSection = section;
        
        let range = '';
        let district = ''; 
        
        let rank = '';
        let station = '';

        // A. Split Designation by first comma
        const commaIdx = rawDesig.indexOf(',');
        const knownRanks = [
            'DG & IGP', 'DGP', 'ADGP', 'IGP', 'DIG', 'DCP', 'SP', 'ACP', 'DSP', 'ADDL_SP', 'PI', 'PSI', 'ASI', 'HC', 'PC', 
            'FDA', 'SDA', 'PRO', 'APRO', 'PA TO', 'PATO', 'PA', 'AAO', 'AO', 'CAO', 'OS', 'SS', 'Superintendent', 'Manager',
            'Cmdt.', 'Dy. Cmdt.', 'Asst. Cmdt.'
        ];
        
        if (commaIdx !== -1) {
            rank = rawDesig.substring(0, commaIdx).trim();
            station = rawDesig.substring(commaIdx + 1).trim();
        } else {
            let rankFound = false;
            for (const kr of knownRanks) {
                if (rawDesig.toUpperCase().startsWith(kr + ' ') || rawDesig.toUpperCase() === kr) {
                    rank = rawDesig.substring(0, kr.length).trim();
                    station = rawDesig.substring(kr.length).trim();
                    rankFound = true;
                    break;
                }
            }
            if (!rankFound) {
                // 🕵️‍♂️ Robust Retired Officers Deep Clean
                if (originalUnit === 'Retired' || originalUnit.includes('Retired') || originalUnit.includes('Retired Police Officers') || rawDesig.toUpperCase().includes('RETD')) {
                    let cleanDesig = rawDesig.replace(/RETD\.?|RETIRED|\(Retd\.?\)/gi, '').replace(/\s+/g, ' ').trim();
                    
                    // 1. Try to find a known rank in the cleaned string
                    let detectedRank = '';
                    for (const kr of knownRanks) {
                        const regex = new RegExp(`\\b${kr}\\b`, 'i');
                        if (regex.test(cleanDesig)) {
                            detectedRank = kr;
                            cleanDesig = cleanDesig.replace(regex, '').replace(/,/g, '').trim();
                            break;
                        }
                    }
                    
                    // 2. The remaining part is the Name
                    // Handle cases like "Spl. Secret Govt. of Ind" by cleaning noise
                    const noise = ['Spl. Secret', 'Govt. of Ind', 'Spl. Sec', 'Cabinet Secretariat', 'RERA', 'KSPCB', 'KUIDFC'];
                    noise.forEach(n => {
                        const nr = new RegExp(n, 'gi');
                        cleanDesig = cleanDesig.replace(nr, '');
                    });
                    
                    station = `${cleanDesig.replace(/,/g, '').trim()} (Retd.)`;
                    rank = detectedRank || 'Officer';
                    rankFound = true;
                }
            }

            if (!rankFound) {
                station = rawDesig;
                rank = '';
            }
        }

        let shortRank = rank;
        for (const [long, short] of Object.entries(rankShortMap)) {
            if (rank === long || rank.includes(long)) {
                shortRank = short;
                break;
            }
        }

        if (originalSection === 'State Police Headquarters') {
            range = '';
            if (originalUnit === 'Headquarters') {
                section = 'CHIEF OFFICE';
                unit = 'HQ'; // Default fallback
                
                // 🎯 Prioritize specific Unit keywords from Designation
                for (const key of hqKeys) {
                    if (new RegExp(`\\b${key.replace('/', '\\/')}\\b`, 'i').test(rawDesig)) {
                        unit = hqKeywordMap[key];
                        break;
                    }
                }
            } else {
                section = 'SPHQ';
                // Apply Unit mapping for SPHQ units
                let matched = false;
                for (const [long, short] of Object.entries(unitShortMap)) {
                    if (originalUnit === long || originalUnit.includes(long)) {
                        unit = short;
                        matched = true;
                        break;
                    }
                }
                if (!matched) unit = originalUnit;
            }
            for (const key of hqKeys) {
                if (new RegExp(`\\b${key.replace('/', '\\/')}\\b`, 'i').test(rawDesig)) {
                    const mappedSection = hqKeywordMap[key];
                    if (mappedSection === 'C/Room') {
                        unit = 'C/Room';
                    }
                    break;
                }
            }
        } else if (originalSection === 'Special Units') {
            section = 'SPECIAL UNITS';
            range = '';
            let matched = false;
            for (const [long, short] of Object.entries(unitShortMap)) {
                if (originalUnit === long || originalUnit.includes(long)) {
                    unit = short;
                    matched = true;
                    break;
                }
            }
            if (!matched) unit = originalUnit;
            const lowerDesig = rawDesig.toLowerCase();
            if (unit === 'Intelligence' || lowerDesig.includes(' int ') || lowerDesig.includes(' intelligence')) {
                section = 'SPECIAL UNITS';
                unit = 'State INT';
            }
        } else if (originalSection === 'Districts') {
            section = 'RANGES';
            unit = originalUnit;
            district = originalUnit;
            range = getRangeForDistrict(unit);
        } else if (originalSection === 'Ranges') {
            section = 'RANGES';
            for (const rk of rangeKeywords) {
                if (originalUnit.includes(rk)) {
                    range = rk.includes('Range') ? rk : rk + ' Range';
                    break;
                }
            }
            unit = originalUnit;
        } else if (originalSection === 'Karnataka Railways Police') {
            section = 'RAILWAYS';
            unit = 'Railways';
            // Extract district/range from the original unit name (e.g., "Hubballi-Dharwad City")
            district = originalUnit;
            range = getRangeForDistrict(district);
        } else if (originalSection === 'Commissionerates') {
            section = 'COMMISSIONERATES';
            range = originalUnit;
            unit = originalUnit;
            district = originalUnit;
        } else {
            section = originalSection.toUpperCase();
            unit = originalUnit;
        }

        if (section === 'SPECIAL UNITS' || section === 'CHIEF OFFICE') {
            const lowerDesig = rawDesig.toLowerCase();
            if (unit === 'Headquarters' || unit === 'C/Room' || unit === '' || unit === 'HQ') {
                if (lowerDesig.includes('division') || lowerDesig.includes('cell') || lowerDesig.includes('district')) {
                     if (lowerDesig.includes('admin') || lowerDesig.includes('chief') || lowerDesig.includes('dg & igp')) {
                         unit = 'HQ';
                     } else {
                         unit = 'Districts';
                     }
                } else if (unit !== 'C/Room') {
                    unit = 'HQ';
                }
            }
        }

        const desigUpper = rawDesig.toUpperCase();
        if (desigUpper.includes('C/ROOM') || desigUpper.includes('CONTROL ROOM')) {
            if (section === 'CHIEF OFFICE' || section === 'STATE POLICE HEADQUARTERS') {
                unit = 'C/Room';
            }
        }

        const rawPhones = row.Phone || '';
        const landlines = rawPhones.split(',').map(p => p.split('/')[0].trim()).filter(p => p && p !== '-');
        const mobiles = rawPhones.split(',').map(p => {
            const m = p.split('/')[1];
            return m ? m.trim() : '';
        }).filter(m => m && m !== '-');

        const emails = (row.Email || '').split(/,|\/|\n/).map(e => e.trim()).filter(e => e);

        if (district === '') {
            for (const [key, val] of Object.entries(districtMap)) {
                if (station.toLowerCase().includes(key.toLowerCase()) || unit.toLowerCase().includes(key.toLowerCase())) {
                    district = val;
                    break;
                }
            }
        }

        // 🎯 Final Range Check: If range is blank but district is known, fill it!
        if (range === '' && district !== '') {
            range = getRangeForDistrict(district);
        }

        if (station.toLowerCase() === 'office') station = '';

        const agid = `KSP${String(index + 1).padStart(4, '0')}`;
        // 🎯 Name Formatting (Designation-focused for Commandants)
        let displayName = (originalUnit === 'Retired') ? station : (shortRank ? (station ? `${shortRank}, ${station}` : shortRank) : station);
        
        // Use full designation if it's a Commandant or specialized post
        if (rawDesig.toUpperCase().includes('COMMANDANT') || rawDesig.toUpperCase().includes('CMDT')) {
            const unitSuffix = unit.includes('BN') ? unit : '';
            displayName = `Commandant, ${station || unit} ${unitSuffix}`.trim();
        }
        
        if (displayName.startsWith(', ')) displayName = displayName.substring(2);

        // 🎯 Smart District/Range Detection (from Unit/Station name)
        if (!district) {
            for (const d of Object.keys(districtToRangeMap)) {
                if ((rawDesig + ' ' + originalUnit).includes(d)) {
                    district = d;
                    range = districtToRangeMap[d];
                    break;
                }
            }
        }

        // 🎯 Detect Sub Division & Range Override
        let subDivision = '';
        const sdKeywords = ['North', 'South', 'East', 'West', 'Central', 'North East', 'South East', 'North West', 'South West', 'Whitefield', 'Railway', 'BBMP', 'CCB', 'CAR', 'RPF'];
        const sdMatch = (rawDesig + ' ' + originalUnit).match(/(?:[A-Za-z\s]+)\sSub-?Division/i);
        
        if (sdMatch) {
            subDivision = sdMatch[0].trim();
        } else {
            // Check for directional keywords as fallback, but avoid "Range"
            for (const sdk of sdKeywords) {
                // Handle "North" and "Northern" etc for Range
                const rangeRegex = new RegExp(`\\b${sdk}(?:ern)?\\s+Range\\b`, 'i');
                const rangeMatch = rawDesig.match(rangeRegex);
                if (rangeMatch) {
                    range = rangeMatch[0].trim();
                    // Fix double "Range Range"
                    if (range.toLowerCase().includes('range range')) {
                        range = range.replace(/range range/gi, 'Range');
                    }
                    break;
                }
                const sdKeywordMatch = rawDesig.match(new RegExp(`\\b${sdk}\\b`, 'i'));
                if (sdKeywordMatch) {
                    subDivision = sdk;
                    break;
                }
            }
        }

        let wing = 'L&O'; // Default
        if (originalSection === 'State Police Headquarters') {
            if (unit === 'On Deputation') {
                wing = 'On Deputation';
            } else if (originalUnit === 'Headquarters') {
                wing = 'Admin';
            } else {
                wing = 'KSP HQ';
            }
        } else if (originalSection === 'Special Units') {
            if (unit === 'CID') wing = 'CID';
            else if (unit === 'ISD') wing = 'ISD';
            else if (unit === 'KSRP' || unit === 'IRB') wing = 'KSRP';
            else if (unit === 'Intelligence' || unit === 'State INT') wing = 'Intelligence';
            else if (['FPB', 'FSL', 'Computer Wing'].includes(unit)) wing = 'Crime & Technical';
            else if (unit === 'SCRB') wing = 'SCRB';
            else if (unit === 'DCRE') wing = 'DCRE';
            else if (unit === 'Training') wing = 'Training';
            else if (unit === 'C&TS') wing = 'Communication & Logistics';
            else wing = unit; // Use unit name as Wing instead of "Specialized"
        } else if (originalSection === 'Karnataka Railways Police') {
            wing = 'Karnataka Railways Police';
        } else if (originalSection === 'Retired Police Officers') {
            wing = 'Retired';
        }

        // 🎯 Override Wing based on Name Keywords (Priority)
        const upperName = displayName.toUpperCase();
        if (upperName.includes('CID ')) wing = 'CID';
        else if (upperName.includes('ISD ')) wing = 'ISD';
        else if (upperName.includes('C&TS')) wing = 'Communication & Logistics';
        else if (upperName.includes('SCRB')) wing = 'SCRB';
        else if (upperName.includes('KSRP ')) wing = 'KSRP';
        else if (upperName.includes('RAILWAY')) wing = 'Karnataka Railways Police';
        else if (upperName.includes('INTELLIGENCE ')) wing = 'Intelligence';
        else if (upperName.includes('TRAFFIC ')) wing = 'Traffic';

        return {
            agid: agid,
            Wing: wing,
            Unit: unit,
            Range: (range === unit) ? '' : range,
            District: (district === unit) ? '' : district,
            'Sub Division': subDivision,
            Section: '', // We'll use this for Work Type if needed, otherwise blank to avoid repetition
            Name: displayName,
            Rank: shortRank,
            Station: (station === displayName || station === unit) ? '' : station,
            'Office 1': applySTD(landlines[0] || '', district || unit),
            'Office 2': applySTD(landlines[1] || '', district || unit),
            'Mobile 1': mobiles[0] || '',
            'Mobile 2': mobiles[1] || '',
            'Email 1': emails[0] || '',
            'Email 2': emails[1] || ''
        };
    });

    const seen = new Set();
    const finalData = allOfficers.filter(r => {
        const key = `${r.Wing}|${r.Unit}|${r.Name}|${r['Mobile 1']}`.toLowerCase();
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
    });

    // 🎯 Final Multi-Level Sort: Wing -> Seniority (Rank) -> District -> Name
    finalData.sort((a, b) => {
        if (a.Wing !== b.Wing) return a.Wing.localeCompare(b.Wing);
        const wa = getRankWeight(a.Rank);
        const wb = getRankWeight(b.Rank);
        if (wa !== wb) return wa - wb;
        if (a.District !== b.District) return a.District.localeCompare(b.District);
        return a.Name.localeCompare(b.Name);
    });

    const headers = ['agid', 'Unit', 'Range', 'District', 'Sub Division', 'Section', 'Name', 'Rank', 'Station', 'Office 1', 'Office 2', 'Mobile 1', 'Mobile 2', 'Email 1', 'Email 2'];
    
    // 🎯 Generate Excel with Multi-Sheets (Master + Unit-wise)
    const workbook = new ExcelJS.Workbook();
    
    // 1. Master Merged Sheet
    const masterSheet = workbook.addWorksheet('MASTER_MERGED', {
        views: [{ state: 'frozen', xSplit: 1, ySplit: 1 }] // Frozen first column (Unit)
    });
    masterSheet.addRow(headers);
    finalData.forEach(r => masterSheet.addRow(headers.map(h => r[h])));
    styleSheet(masterSheet);

    // 2. Unit-wise Sheets
    const units = [...new Set(finalData.map(r => r.Unit))].sort();
    units.forEach(unitName => {
        // Excel sheet names cannot exceed 31 chars and cannot have special chars
        const safeSheetName = unitName.substring(0, 31).replace(/[\\\?\*\[\]\:\/]/g, '');
        const unitSheet = workbook.addWorksheet(safeSheetName, {
            views: [{ state: 'frozen', xSplit: 1, ySplit: 1 }]
        });
        unitSheet.addRow(headers);
        const unitData = finalData.filter(r => r.Unit === unitName);
        unitData.forEach(r => unitSheet.addRow(headers.map(h => r[h])));
        styleSheet(unitSheet);
    });

    await workbook.xlsx.writeFile(OUTPUT_EXCEL);
    console.log(`Successfully generated V3 files with ${units.length} Unit sheets.`);
    console.log(`Excel: ${OUTPUT_EXCEL}`);

    // Helper for styling
    function styleSheet(sheet) {
        sheet.getRow(1).font = { bold: true };
        sheet.columns.forEach(column => {
            column.width = 20;
        });
        sheet.autoFilter = {
            from: { row: 1, column: 1 },
            to: { row: 1, column: headers.length }
        };
    }

    // Generate CSV for App (Align with Employee.kt model)
    const csvContent = [
        ['agid', 'name', 'rank', 'station', 'unit', 'district', 'subDivision', 'landline', 'mobile', 'email', 'email2', 'category'].join(','),
        ...finalData.map(r => [
            r.agid,
            `"${r.Name.replace(/"/g, '""')}"`,
            `"${r.Rank.replace(/"/g, '""')}"`,
            `"${r.Station.replace(/"/g, '""')}"`,
            `"${r.Section.replace(/"/g, '""')}"`,
            `"${r.District.replace(/"/g, '""')}"`,
            `"${(r['Sub Division'] || '').replace(/"/g, '""')}"`,
            `"${r['Office 1'].replace(/"/g, '""')}"`,
            `"${r['Mobile 1'].replace(/"/g, '""')}"`,
            `"${r['Email 1'].replace(/"/g, '""')}"`,
            `"${r['Email 2'].replace(/"/g, '""')}"`,
            `"${r.Wing.replace(/"/g, '""')}"`
        ].join(','))
    ].join('\n');
    fs.writeFileSync(OUTPUT_APP_CSV, csvContent);
    console.log(`CSV: ${OUTPUT_APP_CSV}`);
})();
