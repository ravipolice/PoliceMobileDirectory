const XLSX = require('xlsx');

const excelFilePath = "c:\\Users\\ravip\\AndroidStudioProjects\\PoliceMobileDirectory\\KSP_Contacts_UnitWise.xlsx";

function run() {
  console.log(`Reading Excel file: ${excelFilePath}...`);
  const workbook = XLSX.readFile(excelFilePath);
  
  const sheetNames = workbook.SheetNames;
  const firstSheetName = sheetNames[0];
  const worksheet = workbook.Sheets[firstSheetName];
  
  // Convert sheet to JSON starting from row 1 (which has the headers)
  // Let's first inspect raw headers by converting the first few rows
  const rawData = XLSX.utils.sheet_to_json(worksheet, { header: 1 });
  
  console.log('Total rows in UnitWise:', rawData.length);
  
  // Since row 0 might be a title row, let's look at row 0 and row 1
  console.log('Row 0:', rawData[0]);
  console.log('Row 1 (headers):', rawData[1]);
  
  const headers = rawData[1] || [];
  const stationIndex = headers.indexOf('Station');
  console.log('Station column index:', stationIndex);
  
  if (stationIndex === -1) {
    console.log('No "Station" column found in Excel headers');
  } else {
    let nonEmptyCount = 0;
    const sampleValues = [];
    for (let i = 2; i < rawData.length; i++) {
      const row = rawData[i];
      const val = row[stationIndex];
      if (val !== undefined && val !== null && String(val).trim() !== '') {
        nonEmptyCount++;
        if (sampleValues.length < 15) {
          sampleValues.push(val);
        }
      }
    }
    console.log(`Number of rows with a non-empty Station field: ${nonEmptyCount}`);
    console.log(`Sample Station values:`, sampleValues);
  }
  
  process.exit(0);
}

run();
