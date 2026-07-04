const admin = require('firebase-admin');
const path = require('path');

const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function countOld() {
  const snapshot = await db.collection('officers').get();
  let count = 0;
  snapshot.forEach(doc => {
    if (!doc.data().searchBlob) count++;
  });
  console.log('Total documents in officers:', snapshot.size);
  console.log('Records missing searchBlob (Stale data):', count);
  process.exit(0);
}

countOld().catch(console.error);
