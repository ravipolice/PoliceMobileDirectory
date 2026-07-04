const fs = require('fs');
const XLSX = require('xlsx');

const rawExcelPath = "c:\\Users\\ravip\\AndroidStudioProjects\\PoliceMobileDirectory\\KSP_Contacts_Final_Directory_V3_CLEAN.xlsx";
const processedCsvPath = "c:\\Users\\ravip\\AndroidStudioProjects\\PoliceMobileDirectory\\KSP_Officers_App_FINAL.csv";

const rankPrefixes = [
    "Director General & Inspector General of Police",
    "Director General of Police",
    "Additional Director General",
    "Additional Director",
    "Additional Superintendent of Police",
    "Additional Inspector General of Police",
    "Additional Inspector General",
    "Inspector General of Police",
    "Deputy Inspector General of Police",
    "Deputy Inspector General",
    "Superintendent of Police",
    "Deputy Superintendent of Police",
    "Assistant Superintendent of Police",
    "Deputy Commissioner of Police",
    "Assistant Commissioner of Police",
    "Circle Police Inspector",
    "Police Inspector",
    "Police Sub-Inspector",
    "Assistant Police Inspector",
    "DG & IGP", "DG&IGP",
    "Addl. IG", "Addl. SP-1", "Addl. SP-2", "Addl. SP", "Addl. DG", "Addl. DCP",
    "AC",
    "ADGP", "DGP",
    "IGP", "DIGP",
    "SP (W)", "SP(W)", "DSP (W)", "DSP(W)", "PI (W)", "PI(W)", "ACP (W)", "ACP(W)",
    "SP", "DSP", "ASP",
    "DCP", "ADCP",
    "ACP",
    "CPI",
    "SDPO",
    "DySP",
    "PI(W)-1", "PI(W)-2", "PI(W)-3",
    "PI",
    "PSI-1", "PSI-2", "PSI-3", "PSI-4", "PSI",
    "SI", "ASI",
    "SHO",
    "HC", "PC",
    "HM",
    "RPI-1", "RPI-2", "RPI-3", "RPI",
    "RSI",
    "Vice Principal", "Principal",
    "6th BN Commdt.",
    "AAO", "AO", "FAO",
    "CAO", "APRO",
    "PA to", "P.A. to",
    "Secretary",
    "Member Secretary",
    "PA"
];

function splitDesignation(desig) {
  desig = desig.trim();
  if (desig.match(/RETD\./i)) {
    return { rank: '', station: '', isRetired: true };
  }
  for (const rank of rankPrefixes) {
    const escaped = rank.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
    const regex = new RegExp('^(' + escaped + ')\\s*,?\\s+(.+)$', 'i');
    const match = desig.match(regex);
    if (match) {
      return { rank: match[1].trim(), station: match[2].trim().replace(/^,/, '').trim(), isRetired: false };
    }
    if (desig.toLowerCase() === rank.toLowerCase()) {
      return { rank: rank, station: '', isRetired: false };
    }
  }
  const match2 = desig.match(/^(PSI-\d+)\s+(.+)$/i);
  if (match2) {
    return { rank: match2[1], station: match2[2].trim(), isRetired: false };
  }
  return { rank: '', station: desig, isRetired: false };
}

function run() {
  // 1. Raw Excel check
  const workbook = XLSX.readFile(rawExcelPath);
  const worksheet = workbook.Sheets[workbook.SheetNames[0]];
  const rawData = XLSX.utils.sheet_to_json(worksheet, { header: 1 });
  const rawHeaders = rawData[0];
  const nameIndex = rawHeaders.indexOf('Name');
  const rankIndex = rawHeaders.indexOf('Rank');
  const unitIndex = rawHeaders.indexOf('UNIT');
  
  const rawStations = new Set();
  
  for (let i = 1; i < rawData.length; i++) {
    const row = rawData[i];
    const unit = row[unitIndex] || '';
    const rankVal = row[rankIndex] || '';
    const nameVal = row[nameIndex] || '';
    
    if (unit.toLowerCase() === 'retired' || rankVal.toLowerCase().includes('retd')) {
      continue;
    }
    
    const split = splitDesignation(nameVal);
    if (split.station) {
      rawStations.add(split.station);
    }
  }
  
  // 2. Processed CSV check
  const csvContent = fs.readFileSync(processedCsvPath, 'utf-8');
  const csvLines = csvContent.split('\n');
  const csvHeaders = csvLines[0].split(',').map(h => h.trim().replace(/^"|"$/g, ''));
  const csvStationIndex = csvHeaders.indexOf('station');
  
  const processedStations = new Set();
  
  for (let i = 1; i < csvLines.length; i++) {
    const line = csvLines[i].trim();
    if (!line) continue;
    
    // Custom CSV parser handling quotes
    const result = [];
    let cur = '';
    let inQuotes = false;
    for (let j = 0; j < line.length; j++) {
      const char = line[j];
      if (char === '"') {
        inQuotes = !inQuotes;
      } else if (char === ',' && !inQuotes) {
        result.push(cur.trim());
        cur = '';
      } else {
        cur += char;
      }
    }
    result.push(cur.trim());
    
    const station = (result[csvStationIndex] || '').replace(/^"|"$/g, '').trim();
    if (station) {
      processedStations.add(station);
    }
  }
  
  console.log(`Excel Unique Stations parsed: ${rawStations.size}`);
  console.log(`CSV Unique Stations: ${processedStations.size}`);
  process.exit(0);
}

run();
