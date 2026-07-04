const admin = require('firebase-admin');
const path = require('path');

// 1. Initialize Firebase Admin
const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const collectionRef = db.collection('officers');

async function cleanupStale() {
    console.log('Fetching stale records (missing searchBlob)...');
    const snapshot = await collectionRef.get();
    
    const staleDocs = [];
    snapshot.forEach(doc => {
        if (!doc.data().searchBlob) {
            staleDocs.push(doc.id);
        }
    });

    console.log(`Found ${staleDocs.length} stale records to delete.`);

    if (staleDocs.length === 0) {
        console.log('Nothing to clean up.');
        process.exit(0);
    }

    try {
        let count = 0;
        let batch = db.batch();
        
        for (const docId of staleDocs) {
            batch.delete(collectionRef.doc(docId));
            count++;
            
            // Commit batch every 500 records
            if (count % 500 === 0) {
                await batch.commit();
                console.log(`Deleted ${count} stale records...`);
                batch = db.batch();
            }
        }
        
        // Commit remaining records
        if (count % 500 !== 0) {
            await batch.commit();
        }
        
        console.log('--- CLEANUP COMPLETE ---');
        console.log(`Successfully removed ${staleDocs.length} stale/duplicate records from the 'officers' collection.`);
        process.exit(0);
    } catch (error) {
        console.error('Cleanup failed:', error);
        process.exit(1);
    }
}

cleanupStale();
