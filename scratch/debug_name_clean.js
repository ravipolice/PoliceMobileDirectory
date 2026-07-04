const xlsx = require('xlsx');
const path = require('path');

const v3Path = path.join(__dirname, '..', 'KSP_Contacts_Final_Directory_V3.xlsx');
const wb = xlsx.readFile(v3Path);
const rows = xlsx.utils.sheet_to_json(wb.Sheets['MASTER_MERGED_FINAL']);

console.log("Debugging name clean for Kalaburagi AAOs:");
rows.forEach((r, idx) => {
    let name = String(r.Name || "").trim();
    let rank = String(r.Rank || "").trim();
    let district = String(r.District || "").trim();
    let unit = String(r.UNIT || "").trim();

    if (rank === "AAO" && (district.includes("Kalaburagi") || district === "")) {
        console.log(`Index: ${idx}, Name: "${name}", Rank: "${rank}", District: "${district}", Unit: "${unit}"`);
        
        const isAAOorAO = rank.toLowerCase() === 'aao' || rank.toLowerCase() === 'ao';
        const isRedundantName = name.toLowerCase() === rank.toLowerCase() ||
                                name === "AAO IGP Office" ||
                                name === "AAO, IGP Office" ||
                                name === "AAO DPO";
        
        console.log(`isAAOorAO: ${isAAOorAO}, isRedundantName: ${isRedundantName}`);
        
        if (isAAOorAO && isRedundantName) {
            let descriptive = rank;
            if (district) {
                if (district.includes("Range")) {
                    const rangePart = district.split(/[-–,/]/)[0].trim();
                    descriptive = rank + " " + rangePart;
                } else {
                    descriptive = rank + " " + district;
                }
            } else if (unit && unit !== 'Others' && unit !== 'L&O') {
                descriptive = rank + " " + unit;
            }
            console.log(`Descriptive Name resolved: "${descriptive.trim()}"`);
        }
    }
});
