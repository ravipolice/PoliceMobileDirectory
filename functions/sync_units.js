const { initializeApp, cert } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const path = require('path');

const serviceAccount = require(path.join(__dirname, '..', 'service-account.json'));

initializeApp({
  credential: cert(serviceAccount)
});

const db = getFirestore();

const officialUnits = [
  // State HQ Level (mappingType: state)
  { name: "Admin", mappingType: "state", isHqLevel: true, scopes: ["HQ (State Level)"] },
  { name: "CID", mappingType: "state", isHqLevel: true, scopes: ["HQ (State Level)"] },
  { name: "Intelligence", mappingType: "state", isHqLevel: true, scopes: ["HQ (State Level)"] },
  { name: "Home", mappingType: "state", isHqLevel: true, scopes: ["HQ (State Level)"] },
  { name: "Recruitment", mappingType: "state", isHqLevel: true, scopes: ["HQ (State Level)"] },
  { name: "Training", mappingType: "state", isHqLevel: true, scopes: ["HQ (State Level)"] },
  { name: "Wireless", mappingType: "state", isHqLevel: true, scopes: ["HQ (State Level)"] },
  { name: "Computer", mappingType: "state", isHqLevel: true, scopes: ["HQ (State Level)"] },
  { name: "FSL", mappingType: "state", isHqLevel: true, scopes: ["HQ (State Level)"] },
  { name: "FPB", mappingType: "state", isHqLevel: true, scopes: ["HQ (State Level)"] },
  { name: "SCRB", mappingType: "state", isHqLevel: true, scopes: ["HQ (State Level)"] },
  { name: "Lokayukta", mappingType: "state", isHqLevel: true, scopes: ["HQ (State Level)"] },

  // Regional/District Level (mappingType: district)
  { name: "L&O", mappingType: "district", isDistrictLevel: true, scopes: ["District Level", "Commissionerate"] },
  { name: "KSRP", mappingType: "district", isDistrictLevel: true, scopes: ["District Level", "Battalion"] },
  { name: "ISD", mappingType: "district", isDistrictLevel: true, scopes: ["District Level"] },
  { name: "DCRE", mappingType: "district", isDistrictLevel: true, scopes: ["District Level"] },
  { name: "Railway", mappingType: "district", isDistrictLevel: true, scopes: ["District Level"] },
  { name: "CSP", mappingType: "district", isDistrictLevel: true, scopes: ["District Level"] },
  { name: "BMTF", mappingType: "district", isDistrictLevel: true, scopes: ["District Level"] },
  { name: "SIT", mappingType: "district", isDistrictLevel: true, scopes: ["District Level"] },
  { name: "STF", mappingType: "district", isDistrictLevel: true, scopes: ["District Level"] },
  { name: "SAF", mappingType: "district", isDistrictLevel: true, scopes: ["District Level"] },
  { name: "Sports", mappingType: "district", isDistrictLevel: true, scopes: ["District Level"] },
  { name: "IRB", mappingType: "district", isDistrictLevel: true, scopes: ["District Level", "Battalion"] },
  { name: "Traffic", mappingType: "district", isDistrictLevel: true, scopes: ["District Level", "Commissionerate"] },

  // General/Others
  { name: "Control Room", mappingType: "all", isActive: true, scopes: ["All"] },
  { name: "Others", mappingType: "all", isActive: true, scopes: ["All"] },
  { name: "Retired", mappingType: "all", isActive: true, scopes: ["All"] }
];

async function syncUnits() {
    console.log("Starting Firestore Unit Synchronization...");
    const unitsCol = db.collection('units');
    
    // 1. Delete existing units
    console.log("Deleting legacy units...");
    const snap = await unitsCol.get();
    const batch = db.batch();
    snap.docs.forEach(doc => batch.delete(doc.ref));
    await batch.commit();
    console.log(`Deleted ${snap.size} legacy units.`);

    // 2. Add fresh standardized units
    console.log("Adding standardized units...");
    for (const unit of officialUnits) {
        await unitsCol.add({
            ...unit,
            isActive: true,
            createdAt: new Date()
        });
        console.log(`Added unit: ${unit.name}`);
    }

    console.log("Unit Synchronization complete!");
}

syncUnits().catch(console.error);
