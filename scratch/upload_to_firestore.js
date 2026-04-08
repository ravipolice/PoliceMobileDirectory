const admin = require('firebase-admin');
const fs = require('fs');
const { parse } = require('csv-parse/sync');

// 1. Initialize Firebase Admin
const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const collectionRef = db.collection('officers_v2');

// 2. Read and Parse CSV
const csvPath = '../KSP_Officers_App.csv';
const fileContent = fs.readFileSync(csvPath, 'utf8');

const records = parse(fileContent, {
  columns: true,
  skip_empty_lines: true
});

console.log(`Starting Firestore upload of ${records.length} records to collection 'officers_v2'...`);

// 3. Batch Upload to Firestore (Max 500 per batch)
async function uploadToFirestore() {
    try {
        let count = 0;
        let batch = db.batch();
        
        for (const record of records) {
            // Use AGID as the Document ID
            const docRef = collectionRef.doc(record.agid);
            batch.set(docRef, record);
            
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
        
        console.log('--- FIRESTORE UPLOAD COMPLETE ---');
        console.log(`Successfully uploaded ${records.length} officers to Firestore collection 'officers_v2'.`);
        process.exit(0);
    } catch (error) {
        console.error('Firestore upload failed:', error);
        process.exit(1);
    }
}

uploadToFirestore();
