const admin = require('firebase-admin');
const serviceAccount = require('../serviceAccountKey.json');

if (admin.apps.length === 0) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

const db = admin.firestore();

async function run() {
  const doc1 = await db.collection('officers').doc('AGID0001').get();
  const doc2 = await db.collection('officers').doc('KSP0321').get();
  
  console.log('=== AGID0001 ===');
  console.log(JSON.stringify(doc1.data(), null, 2));
  
  console.log('\n=== KSP0321 ===');
  console.log(JSON.stringify(doc2.data(), null, 2));
  
  process.exit(0);
}

run().catch(console.error);
