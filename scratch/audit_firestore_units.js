const { initializeApp, cert } = require('firebase-admin/app');
const { getFirestore } = require('firebase-admin/firestore');
const path = require('path');

const serviceAccount = require(path.join(__dirname, '..', 'serviceAccountKey.json'));

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
    console.log(JSON.stringify(units, null, 2));
}

auditFirestoreUnits().catch(console.error);
