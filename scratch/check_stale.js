const admin = require('firebase-admin');
const path = require('path');

const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function checkStale() {
  const snapshot = await db.collection('officers').get();
  let count = 0;
  snapshot.forEach(doc => {
    const data = doc.data();
    if (!data.searchBlob) {
        if (count < 5) {
            console.log(`Stale Doc ${doc.id}:`, JSON.stringify(data, null, 2));
        }
        count++;
    }
  });
  console.log('Total Stale:', count);
  process.exit(0);
}

checkStale().catch(console.error);
