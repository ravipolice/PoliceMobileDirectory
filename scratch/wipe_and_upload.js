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

async function deleteCollection(collectionPath, batchSize) {
    const collectionRef = db.collection(collectionPath);
    const query = collectionRef.orderBy('__name__').limit(batchSize);

    return new Promise((resolve, reject) => {
        deleteQueryBatch(db, query, resolve).catch(reject);
    });
}

async function deleteQueryBatch(db, query, resolve) {
    const snapshot = await query.get();

    const batchSize = snapshot.size;
    if (batchSize === 0) {
        // When there are no documents left, we are done
        resolve();
        return;
    }

    // Delete documents in a batch
    const batch = db.batch();
    snapshot.docs.forEach((doc) => {
        batch.delete(doc.ref);
    });
    await batch.commit();

    // Recurse on the next process tick, to avoid
    // exploding the stack.
    process.nextTick(() => {
        deleteQueryBatch(db, query, resolve);
    });
}

async function wipeAndUpload() {
    console.log('🚀 WARNING: Wiping production "officers" collection...');
    await deleteCollection('officers', 500);
    console.log('✅ Collection wiped successfully.');

    console.log(`🚀 Starting FRESH UPLOAD of ${records.length} records...`);
    
    let count = 0;
    let batch = db.batch();
    
    for (const record of records) {
        // Use agid as docId for the fresh start to keep it consistent
        const docRef = collectionRef.doc(record.agid);
        
        const { docId, ...dataToSave } = record;
        batch.set(docRef, dataToSave);
        
        count++;
        
        if (count % 500 === 0) {
            await batch.commit();
            console.log(`Uploaded ${count} records...`);
            batch = db.batch();
        }
    }
    
    if (count % 500 !== 0) {
        await batch.commit();
    }
    
    console.log('--- FRESH PRODUCTION DEPLOYMENT COMPLETE ---');
    console.log(`Total Records: ${records.length}`);
    process.exit(0);
}

wipeAndUpload().catch(err => {
    console.error('Deployment failed:', err);
    process.exit(1);
});
