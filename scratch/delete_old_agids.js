const admin = require('firebase-admin');
const serviceAccount = require('../serviceAccountKey.json');

if (admin.apps.length === 0) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

const db = admin.firestore();

async function run() {
  console.log('=== Starting Deletion of Stale AGID Documents ===');
  
  const snap = await db.collection('officers').get();
  console.log(`Total documents in 'officers' collection: ${snap.size}`);
  
  const docsToDelete = [];
  snap.forEach(doc => {
    if (doc.id.startsWith('AGID')) {
      docsToDelete.push(doc.ref);
    }
  });
  
  console.log(`Found ${docsToDelete.length} AGID documents to delete.`);
  
  if (docsToDelete.length === 0) {
    console.log('No AGID documents found to delete.');
    process.exit(0);
  }
  
  let count = 0;
  let batch = db.batch();
  
  for (const docRef of docsToDelete) {
    batch.delete(docRef);
    count++;
    
    if (count % 500 === 0) {
      await batch.commit();
      console.log(`Deleted ${count} documents...`);
      batch = db.batch();
    }
  }
  
  if (count % 500 !== 0) {
    await batch.commit();
  }
  
  console.log(`Successfully deleted all ${count} stale AGID documents.`);
  
  // Verify final count
  const finalSnap = await db.collection('officers').get();
  console.log(`Final count of documents in 'officers' collection: ${finalSnap.size}`);
  
  process.exit(0);
}

run().catch(console.error);
