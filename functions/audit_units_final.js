const { initializeApp, cert } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const path = require('path');

// Root has service-account.json
const serviceAccount = require(path.join(__dirname, '..', 'service-account.json'));

initializeApp({
  credential: cert(serviceAccount)
});

const db = getFirestore();

async function auditFirestoreUnits() {
    console.log("Fetching units from Firestore...");
    const snap = await db.collection('units').get();
    const units = snap.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
    }));
    
    console.log(`Total units in Firestore: ${units.length}`);
    const uniqueUnitNames = [...new Set(units.map(u => u.name))].sort();
    console.log("Unique Unit Names in Firestore:");
    console.log(JSON.stringify(uniqueUnitNames, null, 2));
    
    // Check for "mixed" names (e.g. ones that aren't in our new short-code list)
    const officialShortCodes = [
      "Admin", "BMTF", "CID", "Computer", "Control Room", "DCRE", "FPB", "FSL", "Forest", "Home",
      "IRB", "ISD", "Intelligence", "KSPH", "KSRP", "L&O", "Lokayukta", "Others", "Prison",
      "Railway", "Recruitment", "Retired", "SAF", "SIT", "STF", "Sports", "Training", "Wireless"
    ];
    
    const mixedUnits = uniqueUnitNames.filter(name => !officialShortCodes.includes(name));
    if (mixedUnits.length > 0) {
        console.log("Found mixed/unnormalized units in Firestore:");
        console.log(JSON.stringify(mixedUnits, null, 2));
    } else {
        console.log("All Firestore units match the official short codes!");
    }
}

auditFirestoreUnits().catch(console.error);
