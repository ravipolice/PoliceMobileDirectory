const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function run() {
  console.log("Reading data dumps...");
  const districtsPath = path.join(__dirname, '../../nandija/allDistrictsDump.json');
  const ranksPath = path.join(__dirname, '../../nandija/allRanksDump.json');

  const districts = JSON.parse(fs.readFileSync(districtsPath, 'utf8'));
  const ranks = JSON.parse(fs.readFileSync(ranksPath, 'utf8'));

  const activeDistricts = districts.filter(d => d.isActive !== false);
  const activeRanks = ranks.filter(r => r.isActive !== false);

  const commissionerateCities = [];
  const battalions = [];
  const standardDistricts = [];

  activeDistricts.forEach(d => {
    const name = d.name.trim();
    const upperName = name.toUpperCase();
    if (upperName.endsWith(" CITY")) {
      commissionerateCities.push(name);
    } else if (upperName.includes("BN") || upperName.includes("IRB") || upperName.includes("BATTALION")) {
      battalions.push(name);
    } else if (upperName.includes("RANGE") || upperName === "HQ" || upperName === "STATE INT") {
      // Skipped from geographic mapping arrays (HQ is special, range and state INT are skipped)
    } else {
      standardDistricts.push(name);
    }
  });

  const allRankIds = activeRanks.map(r => r.id);

  console.log(`Classified Districts:`);
  console.log(`- Standard Districts (${standardDistricts.length}):`, standardDistricts.join(', '));
  console.log(`- Commissionerate Cities (${commissionerateCities.length}):`, commissionerateCities.join(', '));
  console.log(`- Battalions (${battalions.length}):`, battalions.join(', '));
  console.log(`- Total Active Ranks (${allRankIds.length})`);

  console.log("\nFetching all units from Firestore...");
  const unitsSnap = await db.collection('units').get();
  console.log(`Found ${unitsSnap.size} units in database.`);

  const batch = db.batch();
  let updatedCount = 0;

  for (const doc of unitsSnap.docs) {
    const unit = doc.data();
    const unitName = unit.name;
    let payload = {};

    // 1. HQ Level Units
    const hqUnits = ['Admin', 'CID', 'Intelligence', 'Home', 'Recruitment', 'Training', 'Wireless', 'Computer', 'FSL', 'FPB', 'SCRB', 'Lokayukta'];
    // 2. District Level Units
    const districtUnits = ['ISD', 'DCRE', 'Railway', 'CSP', 'BMTF', 'SIT', 'STF', 'SAF', 'Sports'];
    // 3. Commissionerate + District Units
    const cityDistrictUnits = ['L&O', 'Traffic'];
    // 4. Battalions
    const battalionUnits = ['KSRP', 'IRB'];
    // 5. Global Units
    const globalUnits = ['Control Room', 'Others', 'Retired'];

    if (hqUnits.includes(unitName)) {
      payload = {
        scopes: ["hq"],
        mappedAreaIds: ["HQ"],
        mappedDistricts: ["HQ"],
        mappingType: "state",
        mappedAreaType: "HQ",
        applicableRanks: allRankIds,
        isHqLevel: true,
        isDistrictLevel: false
      };
    } else if (districtUnits.includes(unitName)) {
      payload = {
        scopes: ["district"],
        mappedAreaIds: standardDistricts,
        mappedDistricts: standardDistricts,
        mappingType: "subset",
        mappedAreaType: "DISTRICT",
        applicableRanks: allRankIds,
        isHqLevel: false,
        isDistrictLevel: true
      };
    } else if (cityDistrictUnits.includes(unitName)) {
      payload = {
        scopes: ["district", "commissionerate"],
        mappedAreaIds: [...standardDistricts, ...commissionerateCities],
        mappedDistricts: [...standardDistricts, ...commissionerateCities],
        mappingType: "subset",
        mappedAreaType: "CITY",
        applicableRanks: allRankIds,
        isHqLevel: false,
        isDistrictLevel: true
      };
    } else if (battalionUnits.includes(unitName)) {
      payload = {
        scopes: ["battalion"],
        mappedAreaIds: battalions,
        mappedDistricts: battalions,
        mappingType: "subset",
        mappedAreaType: "BATTALION",
        applicableRanks: allRankIds,
        isHqLevel: false,
        isDistrictLevel: false
      };
    } else if (globalUnits.includes(unitName)) {
      payload = {
        scopes: [],
        mappedAreaIds: [],
        mappedDistricts: [],
        mappingType: "all",
        mappedAreaType: "DISTRICT",
        applicableRanks: allRankIds,
        isHqLevel: false,
        isDistrictLevel: false
      };
    } else {
      console.log(`[WARNING] Unknown unit encountered: "${unitName}". Restoring default rank mappings and setting scope to Global.`);
      payload = {
        applicableRanks: allRankIds
      };
    }

    // Apply update to Firestore document (merges fields)
    batch.update(doc.ref, payload);
    updatedCount++;
    console.log(`Prepared update for unit "${unitName}" -> mappedAreaType: ${payload.mappedAreaType || 'N/A'}, areas: ${payload.mappedAreaIds?.length || 0}, ranks: ${payload.applicableRanks?.length || 0}`);
  }

  if (updatedCount > 0) {
    console.log(`Committing batch write for ${updatedCount} units...`);
    await batch.commit();
    console.log("Successfully restored all unit mappings in Firestore!");
  } else {
    console.log("No units updated.");
  }
}

run().then(() => {
  process.exit(0);
}).catch(err => {
  console.error("Execution Error:", err);
  process.exit(1);
});
