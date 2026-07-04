const admin = require('firebase-admin');
const path = require('path');

const serviceAccount = require('../service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function getCount() {
  const snapshot = await db.collection('officers').get();
  console.log('Final Firestore Count:', snapshot.size);
  process.exit(0);
}

getCount().catch(console.error);
