const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// 1. Initialize Firebase Admin
const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const collectionRef = db.collection('officers');

// 2. Read prepared JSON
const dataPath = path.join(__dirname, '..', 'officers_merge_ready.json');
const records = JSON.parse(fs.readFileSync(dataPath, 'utf8'));

async function uploadToRealOfficers() {
    console.log(`Starting production upload of ${records.length} records to collection 'officers'...`);
    
    try {
        let count = 0;
        let batch = db.batch();
        
        for (const record of records) {
            // Use the docId we mapped (either existing random ID or new AGID)
            const docRef = collectionRef.doc(record.docId);
            
            // Remove docId from the data object itself before saving
            const { docId, ...dataToSave } = record;
            
            batch.set(docRef, dataToSave, { merge: true });
            
            count++;
            
            // Commit batch every 500 records
            if (count % 500 === 0) {
                await batch.commit();
                console.log(`Committed ${count} records...`);
                batch = db.batch();
            }
        }
        
        // Commit remaining records
        if (count % 500 !== 0) {
            await batch.commit();
        }
        
        console.log('--- PRODUCTION UPLOAD COMPLETE ---');
        console.log(`Successfully merged ${records.length} officers into the production 'officers' collection.`);
        process.exit(0);
    } catch (error) {
        console.error('Production upload failed:', error);
        process.exit(1);
    }
}

uploadToRealOfficers();
