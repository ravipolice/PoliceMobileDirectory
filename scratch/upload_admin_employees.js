const admin = require('firebase-admin');
const fs = require('fs');
const { parse } = require('csv-parse/sync');

// 1. Initialize Firebase Admin
const serviceAccount = require('../service-account.json');

if (!admin.apps.length) {
    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount)
    });
}

const db = admin.firestore();
const collectionRef = db.collection('admin_employees');

// 2. Read and Parse CSV
const csvPath = 'admin_employees.tsv';
if (!fs.existsSync(csvPath)) {
    console.error(`ERROR: Could not find ${csvPath}. Please make sure you saved it there.`);
    process.exit(1);
}

const fileContent = fs.readFileSync(csvPath, 'utf8');

const records = parse(fileContent, {
  columns: true,
  skip_empty_lines: true,
  delimiter: '\t'
});

console.log(`Starting Firestore upload of ${records.length} records to collection 'admin_employees'...`);

// 3. Batch Upload to Firestore (Max 500 per batch)
async function uploadToFirestore() {
    try {
        let count = 0;
        let batch = db.batch();
        
        for (const record of records) {
            // Find a unique ID. We prefer 'kgid', then 'email', then fallback to a random string
            const docId = record.kgid || record.KGID || record.email || record.Email || db.collection('admin_employees').doc().id;
            
            // Clean up keys to match Android expectations
            const cleanRecord = { ...record };
            cleanRecord.isAdmin = true; // Mark them as admin employees
            cleanRecord.updatedAt = admin.firestore.FieldValue.serverTimestamp();
            
            // Delete empty keys
            for (const key in cleanRecord) {
                if (!key || key.trim() === '') {
                    delete cleanRecord[key];
                }
            }
            
            const docRef = collectionRef.doc(docId);
            batch.set(docRef, cleanRecord, { merge: true });
            
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
        console.log(`Successfully uploaded ${records.length} employees to Firestore collection 'admin_employees'.`);
        process.exit(0);
    } catch (error) {
        console.error('Firestore upload failed:', error);
        process.exit(1);
    }
}

uploadToFirestore();
