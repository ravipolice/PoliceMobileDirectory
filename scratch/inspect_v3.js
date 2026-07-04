const xlsx = require('xlsx');
const path = require('path');

const filePath = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');

try {
    const workbook = xlsx.readFile(filePath);
    console.log("File Name: KSP_Contacts_Final_Directory_V3.xlsx");
    console.log("Total Sheets:", workbook.SheetNames.length);
    console.log("Sheet Names:", workbook.SheetNames);

    workbook.SheetNames.forEach(sheetName => {
        const worksheet = workbook.Sheets[sheetName];
        const data = xlsx.utils.sheet_to_json(worksheet);
        console.log(`\n--- Sheet: ${sheetName} ---`);
        console.log(`Rows: ${data.length}`);
        if (data.length > 0) {
            console.log("Columns:", Object.keys(data[0]));
            console.log("Sample Row:", JSON.stringify(data[0], null, 2));
        }
    });
} catch (error) {
    console.error("Error reading file:", error.message);
}
